/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.security

import java.util.UUID
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
   */
  private fun selectAssertingPartyEntityId(
      registrationId: UUID,
      registration: ExtendedRegistration,
      metadataRepository: AssertingPartyMetadataRepository
  ): String {
    val configured = registration.assertingparty.entityId
    if (configured != null) {
      return configured
    }
    val candidates = metadataRepository.map { it.entityId }
    require(candidates.size == 1) {
      "Ambiguous SAML configuration $registrationId: the metadata describes " +
          "${candidates.size} identity providers $candidates. Set " +
          "security.saml2.relyingparty.registration.$registrationId.assertingparty.entity-id " +
          "to select one of them."
    }
    return candidates.single()
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
    val metadata = resolved.metadataRepository.findByEntityId(resolved.assertingPartyEntityId)
    if (metadata == null) {
      logger.error(
          "SAML registration {}: the metadata no longer describes entity {}.",
          resolved.registrationId,
          resolved.assertingPartyEntityId)
      return null
    }
    return RelyingPartyRegistration.withAssertingPartyMetadata(metadata)
        // Defaults to the asserting party's entity ID, which is not how registrations are keyed.
        .registrationId(resolved.registrationId.toString())
        .entityId(resolved.entityId)
        .signingX509Credentials { it.addAll(resolved.signingCredentials) }
        .build()
  }

  private class ResolvedRegistration(
      val registrationId: UUID,
      val entityId: String,
      val assertingPartyEntityId: String,
      val metadataRepository: AssertingPartyMetadataRepository,
      val signingCredentials: List<Saml2X509Credential>
  )
}
