/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.security

import java.io.File
import java.io.InputStream
import java.net.URI
import java.net.URL
import java.security.cert.X509Certificate
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import net.particify.arsnova.core4.user.internal.ExtendedSaml2RelyingPartyProperties.ExtendedRegistration
import net.shibboleth.shared.component.DestructableComponent
import net.shibboleth.shared.resolver.ResolverException
import net.shibboleth.shared.resource.Resource as MetadataResource
import org.apache.hc.client5.http.config.RequestConfig
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.core5.util.Timeout
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport
import org.opensaml.saml.metadata.resolver.MetadataResolver
import org.opensaml.saml.metadata.resolver.RefreshableMetadataResolver
import org.opensaml.saml.metadata.resolver.filter.MetadataFilter
import org.opensaml.saml.metadata.resolver.filter.impl.SignatureValidationFilter
import org.opensaml.saml.metadata.resolver.impl.AbstractReloadingMetadataResolver
import org.opensaml.saml.metadata.resolver.impl.HTTPMetadataResolver
import org.opensaml.saml.metadata.resolver.impl.ResourceBackedMetadataResolver
import org.opensaml.saml.metadata.resolver.index.MetadataIndex
import org.opensaml.saml.metadata.resolver.index.impl.RoleMetadataIndex
import org.opensaml.security.credential.Credential
import org.opensaml.security.credential.impl.StaticCredentialResolver
import org.opensaml.security.x509.BasicX509Credential
import org.opensaml.security.x509.X509Support
import org.opensaml.xmlsec.config.impl.DefaultSecurityConfigurationBootstrap
import org.opensaml.xmlsec.signature.support.impl.ExplicitKeySignatureTrustEngine
import org.slf4j.LoggerFactory
import org.springframework.core.io.DefaultResourceLoader
import org.springframework.core.io.Resource
import org.springframework.security.saml2.core.OpenSamlInitializationService
import org.springframework.security.saml2.provider.service.registration.AssertingPartyMetadataRepository
import org.springframework.security.saml2.provider.service.registration.OpenSaml5AssertingPartyMetadataRepository

private const val HTTP_TIMEOUT_SECONDS = 10L
private const val CERTIFICATE_EXPIRY_WARNING_DAYS = 90L

private val logger = LoggerFactory.getLogger(Saml2MetadataResolverFactory::class.java)

/**
 * Builds one metadata resolver per SAML registration, each of which re-reads its source on a
 * schedule derived from the document's own `cacheDuration` and `validUntil`. The factory owns the
 * resolvers, so whoever creates it has to [destroy] it.
 *
 * The resolvers are assembled by hand instead of through
 * [OpenSaml5AssertingPartyMetadataRepository.withTrustedMetadataLocation], which always backs the
 * location with a [ResourceBackedMetadataResolver]. That one only re-reads a document whose
 * `lastModified()` moved past the last refresh, and for an `https` location that value is the
 * `Last-Modified` response header -- or the epoch when the server sends none, in which case the
 * timer fires forever without the document ever being read again. [HTTPMetadataResolver] instead
 * issues a conditional request and re-fetches unconditionally when the server offered no validator
 * to condition it on.
 */
class Saml2MetadataResolverFactory(registrations: Map<UUID, ExtendedRegistration>) {
  private val resourceLoader = DefaultResourceLoader()
  private val resolvers = mutableListOf<MetadataResolver>()
  private val sharedHttpClient: CloseableHttpClient?

  init {
    // Registers the parser pool the resolvers are initialized with. Spring's SAML classes do this
    // from their static initializers, but a resolver is built before any of them is touched, and
    // without the pool it fails with an exception naming nothing that points here.
    OpenSamlInitializationService.initialize()
    sharedHttpClient =
        if (registrations.values.any { isHttpLocation(it.assertingparty.metadataUri) }) {
          createHttpClient()
        } else {
          null
        }
  }

  fun create(
      registrationId: UUID,
      registration: ExtendedRegistration
  ): AssertingPartyMetadataRepository {
    val location =
        requireNotNull(registration.assertingparty.metadataUri) {
          "Incomplete SAML configuration $registrationId: no metadata URI"
        }
    val certificates = verificationCertificates(registrationId, registration)
    warnAboutUnverifiedMetadata(registrationId, location, certificates)
    warnAboutExpiringCertificates(registrationId, certificates)
    val resolver = createResolver(location)
    resolver.setId("saml2-metadata-$registrationId")
    resolver.parserPool = XMLObjectProviderRegistrySupport.getParserPool()
    resolver.metadataFilter = createMetadataFilter(certificates)
    // Restricts iteration to identity providers, which is what tells them apart from the service
    // providers a federation aggregate carries as well.
    resolver.setIndexes(setOf<MetadataIndex>(RoleMetadataIndex()))
    resolvers.add(resolver)
    // Whether a failure here stops the application is decided per source, see createHttpResolver.
    resolver.initialize()
    return OpenSaml5AssertingPartyMetadataRepository(resolver)
  }

  /** Re-reads every metadata document instead of waiting for the next scheduled refresh. */
  internal fun refresh() {
    resolvers.filterIsInstance<RefreshableMetadataResolver>().forEach { refresh(it) }
  }

  /**
   * Idempotent, because a resolver's own cleanup is not: each of them holds a timer which has to be
   * cancelled exactly once, and a Spring context can be closed after a caller released the factory
   * by hand.
   */
  fun destroy() {
    resolvers.filterIsInstance<DestructableComponent>().forEach { it.destroy() }
    resolvers.clear()
    sharedHttpClient?.close()
  }

  /** A failed refresh keeps the last good document, which is what the scheduled one does too. */
  private fun refresh(resolver: RefreshableMetadataResolver) {
    try {
      resolver.refresh()
    } catch (e: ResolverException) {
      logger.error("Failed to refresh SAML metadata.", e)
    }
  }

  private fun createResolver(location: String): AbstractReloadingMetadataResolver =
      if (isHttpLocation(location)) {
        createHttpResolver(location)
      } else {
        createResourceResolver(location)
      }

  /**
   * Fail-fast is off for an endpoint but stays on for a local resource. An endpoint which cannot be
   * reached while the application starts is a transient condition OpenSAML recovers from on its own
   * -- a failed refresh reschedules itself at the minimum delay and keeps trying -- so halting
   * would take every other login method down with it over something that fixes itself. A path which
   * does not resolve is a misconfiguration which never will.
   *
   * A file which is missing is fatal either way: [ResourceBackedMetadataResolver]'s constructor
   * throws before `initialize()` is ever reached, so the flag has nothing to suppress.
   */
  private fun createHttpResolver(location: String) =
      HTTPMetadataResolver(checkNotNull(sharedHttpClient), location).apply {
        setFailFastInitialization(false)
      }

  private fun createResourceResolver(location: String) =
      ResourceBackedMetadataResolver(SpringMetadataResource(resourceLoader.getResource(location)))

  /**
   * Verifies the metadata document's own signature against the certificates pinned for the
   * registration. No pinned certificate means no filter: the document is then accepted as read,
   * which is all a location whose transport is already trusted needs.
   *
   * Pairing the pinned certificates with an inline-`KeyInfo` resolver is the point of the
   * construction. The signing certificate travels inside the document's own
   * `<ds:Signature><ds:KeyInfo>`, so the engine takes the key from there and then has to find it
   * among the configured ones; checking a signature against the certificate it arrived with would
   * prove nothing.
   */
  private fun createMetadataFilter(certificates: List<X509Certificate>): MetadataFilter? {
    if (certificates.isEmpty()) {
      return null
    }
    val credentials: List<Credential> = certificates.map { BasicX509Credential(it) }
    val trustEngine =
        ExplicitKeySignatureTrustEngine(
            StaticCredentialResolver(credentials),
            DefaultSecurityConfigurationBootstrap.buildBasicInlineKeyInfoCredentialResolver())
    val filter = SignatureValidationFilter(trustEngine)
    // An aggregate carries one signature over its root, and accepting an unsigned root would let
    // the signature be stripped instead of forged.
    filter.setRequireSignedRoot(true)
    // The filter refuses to run uninitialized, and the resolver does not initialize it for us.
    filter.initialize()
    return filter
  }

  private fun verificationCertificates(
      registrationId: UUID,
      registration: ExtendedRegistration
  ): List<X509Certificate> =
      registration.metadataVerification.credentials.map {
        val location =
            requireNotNull(it.certificateLocation) {
              "Incomplete SAML configuration $registrationId: metadata verification credential " +
                  "without a certificate location"
            }
        X509Support.decodeCertificate(location.contentAsByteArray)
      }

  private fun warnAboutUnverifiedMetadata(
      registrationId: UUID,
      location: String,
      certificates: List<X509Certificate>
  ) {
    if (certificates.isNotEmpty() || !isHttpLocation(location)) {
      return
    }
    logger.warn(
        "SAML registration {}: metadata is fetched from {} without its signature being verified. " +
            "Configure security.saml2.relyingparty.registration.{}.metadata-verification" +
            ".credentials with the certificates the document is signed with.",
        registrationId,
        location,
        registrationId)
  }

  /**
   * A metadata signing certificate outlives almost everything else in this configuration -- five
   * years is a common federation cadence and some run to twenty -- so a rotation is easy to walk
   * past. Missing it degrades quietly: a refresh which fails keeps the last good document, so
   * logins go on working while the metadata silently stops moving, which is the failure the refresh
   * exists to prevent.
   */
  private fun warnAboutExpiringCertificates(
      registrationId: UUID,
      certificates: List<X509Certificate>
  ) {
    val threshold = Instant.now().plus(CERTIFICATE_EXPIRY_WARNING_DAYS, ChronoUnit.DAYS)
    certificates
        .filter { it.notAfter.toInstant().isBefore(threshold) }
        .forEach {
          logger.warn(
              "SAML registration {}: the metadata verification certificate {} expires on {}.",
              registrationId,
              it.subjectX500Principal.name,
              it.notAfter.toInstant())
        }
  }

  private fun createHttpClient(): CloseableHttpClient {
    val requestConfig =
        RequestConfig.custom()
            .setConnectionRequestTimeout(Timeout.ofSeconds(HTTP_TIMEOUT_SECONDS))
            // Without a response timeout an unresponsive identity provider holds the refresh
            // timer's thread indefinitely.
            .setResponseTimeout(Timeout.ofSeconds(HTTP_TIMEOUT_SECONDS))
            .build()
    return HttpClients.custom().setDefaultRequestConfig(requestConfig).build()
  }

  private fun isHttpLocation(location: String?) =
      location != null && (location.startsWith("http://") || location.startsWith("https://"))
}

/**
 * Adapts a Spring [Resource] to the resource abstraction OpenSAML reads a metadata document from.
 * Spring Security carries an equivalent adapter, but it is private to its own builder.
 */
private class SpringMetadataResource(private val resource: Resource) : MetadataResource {
  override fun exists() = resource.exists()

  override fun isFile() = resource.isFile()

  override fun isReadable() = resource.isReadable()

  override fun isOpen() = resource.isOpen()

  override fun getURL(): URL = resource.getURL()

  override fun getURI(): URI = resource.getURI()

  override fun getFile(): File = resource.getFile()

  override fun getInputStream(): InputStream = resource.getInputStream()

  override fun contentLength() = resource.contentLength()

  override fun lastModified() = resource.lastModified()

  override fun createRelativeResource(relativePath: String): MetadataResource =
      SpringMetadataResource(resource.createRelative(relativePath))

  override fun getFilename(): String? = resource.getFilename()

  override fun getDescription(): String = resource.getDescription()
}
