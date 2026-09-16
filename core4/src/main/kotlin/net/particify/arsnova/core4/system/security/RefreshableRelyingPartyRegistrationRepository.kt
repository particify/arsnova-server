/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.security

import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import net.particify.arsnova.core4.user.internal.ExtendedSaml2RelyingPartyProperties
import net.particify.arsnova.core4.user.internal.ExtendedSaml2RelyingPartyProperties.ExtendedRegistration
import org.opensaml.security.x509.X509Support
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.DisposableBean
import org.springframework.security.converter.RsaKeyConverters
import org.springframework.security.saml2.core.Saml2X509Credential
import org.springframework.security.saml2.provider.service.registration.AssertingPartyMetadataRepository
import org.springframework.security.saml2.provider.service.registration.IterableRelyingPartyRegistrationRepository
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration

private val logger =
    LoggerFactory.getLogger(RefreshableRelyingPartyRegistrationRepository::class.java)

/**
 * Assembles a [RelyingPartyRegistration] from the identity provider's metadata on every lookup, so
 * a rotated signing key is picked up without a restart. [Saml2MetadataResolverFactory] keeps that
 * metadata current.
 *
 * Everything which does not come from the metadata -- the relying party's entity ID and its signing
 * credentials -- is read once. Reading it per lookup would put a file read and an RSA key parse on
 * the login path.
 *
 * Iterable because the metadata endpoint serving all registrations at once is only offered for a
 * repository which is.
 */
class RefreshableRelyingPartyRegistrationRepository(
    saml2Properties: ExtendedSaml2RelyingPartyProperties
) : IterableRelyingPartyRegistrationRepository, DisposableBean {
  private val resolverFactory = Saml2MetadataResolverFactory(saml2Properties.registration)
  private val registrations: Map<String, ResolvedRegistration> =
      saml2Properties.registration
          .map { (id, registration) -> resolve(id, registration) }
          .associateBy { it.registrationId.toString() }

  override fun findByRegistrationId(registrationId: String): RelyingPartyRegistration? =
      registrations[registrationId]?.let { build(it) }

  override fun findUniqueByAssertingPartyEntityId(entityId: String): RelyingPartyRegistration? =
      registrations.values.singleOrNull { it.assertingPartyEntityId == entityId }?.let { build(it) }

  override fun iterator(): MutableIterator<RelyingPartyRegistration> =
      registrations.values.mapNotNullTo(mutableListOf()) { build(it) }.iterator()

  override fun destroy() {
    resolverFactory.destroy()
  }

  /** Re-reads the metadata of every registration right away. */
  internal fun refreshMetadata() {
    resolverFactory.refresh()
  }

  private fun resolve(
      registrationId: UUID,
      registration: ExtendedRegistration
  ): ResolvedRegistration {
    val metadataRepository = resolverFactory.create(registrationId, registration)
    return ResolvedRegistration(
        registrationId = registrationId,
        entityId = registration.entityId,
        assertingPartyEntityId =
            selectAssertingPartyEntityId(registrationId, registration, metadataRepository),
        metadataRepository = metadataRepository,
        signingCredentials = signingCredentials(registrationId, registration))
  }

  /**
   * A federation aggregate describes more than one identity provider, and binding to whichever one
   * the document happens to list first is a misconfiguration nobody would notice. Settling the
   * entity ID here turns that into a startup failure, and it keeps a lookup to an indexed search.
   *
   * A document which describes none is a different case: nothing has loaded yet, which an endpoint
   * that was unreachable while the application started recovers from on its own. That one is
   * deferred to [assertingPartyEntityId] instead of being fatal, and reported from there --
   * OpenSAML has already logged its own degraded-state error by this point.
   */
  private fun selectAssertingPartyEntityId(
      registrationId: UUID,
      registration: ExtendedRegistration,
      metadataRepository: AssertingPartyMetadataRepository
  ): String? {
    val configured = registration.assertingparty.entityId
    if (configured != null) {
      return configured
    }
    val candidates = metadataRepository.map { it.entityId }
    require(candidates.size <= 1) {
      "Ambiguous SAML configuration $registrationId: the metadata describes " +
          "${candidates.size} identity providers $candidates. Set " +
          "security.saml2.relyingparty.registration.$registrationId.assertingparty.entity-id " +
          "to select one of them."
    }
    return candidates.singleOrNull()
  }

  private fun signingCredentials(
      registrationId: UUID,
      registration: ExtendedRegistration
  ): List<Saml2X509Credential> =
      registration.signing.credentials.map { c ->
        require(c.privateKeyLocation != null && c.certificateLocation != null) {
          "Incomplete SAML configuration $registrationId"
        }
        val key = RsaKeyConverters.pkcs8().convert(c.privateKeyLocation!!.file.inputStream())
        val certificate = X509Support.decodeCertificate(c.certificateLocation!!.file)
        Saml2X509Credential.signing(key, certificate)
      }

  private fun build(resolved: ResolvedRegistration): RelyingPartyRegistration? {
    val entityId = assertingPartyEntityId(resolved) ?: return null
    val metadata = resolved.metadataRepository.findByEntityId(entityId)
    if (metadata == null) {
      logger.error(
          "SAML registration {}: the metadata no longer describes entity {}.",
          resolved.registrationId,
          entityId)
      return null
    }
    return RelyingPartyRegistration.withAssertingPartyMetadata(metadata)
        // Defaults to the asserting party's entity ID, which is not how registrations are keyed.
        .registrationId(resolved.registrationId.toString())
        .entityId(resolved.entityId)
        .signingX509Credentials { it.addAll(resolved.signingCredentials) }
        .build()
  }

  /**
   * Retries what startup could not settle, because the metadata may have loaded since. The first
   * success is kept: resolving eagerly is what keeps a lookup to an indexed search rather than an
   * iteration over the whole document.
   *
   * Failing here returns null rather than throwing, which leaves the registration unable to
   * authenticate anyone. There is nothing to fall back to: without a document there is no
   * verification certificate, so half a registration would be worse than none.
   */
  private fun assertingPartyEntityId(resolved: ResolvedRegistration): String? {
    resolved.assertingPartyEntityId?.let {
      return it
    }
    val candidates = resolved.metadataRepository.map { it.entityId }
    val entityId = candidates.singleOrNull()
    if (entityId == null) {
      reportUnresolved(resolved, candidates)
    } else {
      resolved.assertingPartyEntityId = entityId
      reportResolved(resolved, entityId)
    }
    return entityId
  }

  /** The count tells the two cases apart: nothing loaded yet, or an aggregate. */
  private fun reportUnresolved(resolved: ResolvedRegistration, candidates: List<String>) {
    if (resolved.unresolvedReported.compareAndSet(false, true)) {
      logger.error(
          "SAML registration {}: the metadata describes {} identity providers {}, so it " +
              "authenticates nobody.",
          resolved.registrationId,
          candidates.size,
          candidates)
    }
  }

  /** Only reached while memoizing, so the recovery of an outage is announced exactly once. */
  private fun reportResolved(resolved: ResolvedRegistration, entityId: String) {
    if (resolved.unresolvedReported.compareAndSet(true, false)) {
      logger.info(
          "SAML registration {}: the metadata now describes entity {}, so it authenticates again.",
          resolved.registrationId,
          entityId)
    }
  }

  private class ResolvedRegistration(
      val registrationId: UUID,
      val entityId: String,
      @Volatile var assertingPartyEntityId: String?,
      val metadataRepository: AssertingPartyMetadataRepository,
      val signingCredentials: List<Saml2X509Credential>
  ) {
    /**
     * Whether the unresolved state has been logged already. Spring's SAML configurer iterates the
     * repository twice while it builds the filter chain and the metadata endpoint iterates it per
     * request, so logging per lookup would turn one outage into a stream of identical lines.
     */
    val unresolvedReported = AtomicBoolean()
  }
}
