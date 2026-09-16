/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.security

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import java.io.IOException
import java.security.cert.X509Certificate
import java.util.UUID
import net.particify.arsnova.core4.TestcontainersConfiguration
import net.particify.arsnova.core4.user.Saml2MetadataValidators
import net.particify.arsnova.core4.user.Saml2TestIdentityProvider
import net.particify.arsnova.core4.user.Saml2TestMetadataServer
import net.particify.arsnova.core4.user.Saml2TestUser
import net.particify.arsnova.core4.user.registerRelyingParty
import net.particify.arsnova.core4.user.relyingPartyProperties
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistrar
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post

private const val REFRESH_REGISTRATION_ID = "d7c1e2f3-4a5b-4c6d-8e9f-0a1b2c3d4e5f"
private const val ROTATION_REGISTRATION_ID = "e8d2f3a4-5b6c-4d7e-9f0a-1b2c3d4e5f60"
private const val STATUS_OK = 200
private const val STATUS_SERVER_ERROR = 500

private val repositoryLogger =
    LoggerFactory.getLogger(RefreshableRelyingPartyRegistrationRepository::class.java) as Logger

private val ROTATION_REJECTED_USER =
    Saml2TestUser(subjectId = "saml-rotation-rejected", mailAddress = "saml.rejected@example.com")

private val ROTATION_ACCEPTED_USER =
    Saml2TestUser(subjectId = "saml-rotation-accepted", mailAddress = "saml.accepted@example.com")

/**
 * Drives the metadata resolvers without a Spring context. The HTTP cases are the ones which matter:
 * a document read from a file is re-read by the stock Spring builder as well, so only a server
 * tells the two apart.
 */
class Saml2MetadataRefreshTests {
  private val registrationId = UUID.fromString(REFRESH_REGISTRATION_ID)
  private lateinit var identityProvider: Saml2TestIdentityProvider
  private var server: Saml2TestMetadataServer? = null
  private var repository: RefreshableRelyingPartyRegistrationRepository? = null
  private val logEvents = ListAppender<ILoggingEvent>()
  private var previousLogLevel: Level? = null

  @BeforeEach
  fun startIdentityProvider() {
    identityProvider = Saml2TestIdentityProvider()
  }

  /** Pinned rather than inherited, so a Spring context started by another test cannot mute it. */
  @BeforeEach
  fun captureRepositoryLog() {
    logEvents.start()
    previousLogLevel = repositoryLogger.level
    repositoryLogger.level = Level.INFO
    repositoryLogger.addAppender(logEvents)
  }

  @AfterEach
  fun releaseResources() {
    repositoryLogger.detachAppender(logEvents)
    repositoryLogger.level = previousLogLevel
    repository?.destroy()
    server?.stop()
  }

  /**
   * The regression test for the reason the resolvers are assembled by hand:
   * `OpenSaml5AssertingPartyMetadataRepository.withTrustedMetadataLocation` re-reads an HTTP
   * location only when its `Last-Modified` header moves forward, and a server which sends none
   * reports the epoch, so the document is never read again. Whoever simplifies the factory back to
   * that builder fails here.
   */
  @Test
  fun shouldRefreshMetadataFromServerWithoutValidators() {
    val repository = createRepository(Saml2MetadataValidators.NONE)
    val before = verificationCertificates(repository)
    identityProvider.rotateSigningKey()
    checkNotNull(server).publish(identityProvider.metadataDocument)
    repository.refreshMetadata()
    val after = verificationCertificates(repository)
    Assertions.assertNotEquals(before, after)
    Assertions.assertEquals(setOf(identityProvider.signingCertificate), after)
  }

  /** Proves the conditional request is real instead of a re-download on every timer tick. */
  @Test
  fun shouldNotRefetchUnchangedMetadata() {
    val repository = createRepository(Saml2MetadataValidators.ETAG)
    val before = verificationCertificates(repository)
    repository.refreshMetadata()
    repository.refreshMetadata()
    Assertions.assertEquals(3, checkNotNull(server).requestCount)
    Assertions.assertEquals(before, verificationCertificates(repository))
  }

  @Test
  fun shouldKeepLastGoodMetadataWhenServerFails() {
    val repository = createRepository(Saml2MetadataValidators.NONE)
    val before = verificationCertificates(repository)
    checkNotNull(server).status = STATUS_SERVER_ERROR
    assertDoesNotThrow { repository.refreshMetadata() }
    Assertions.assertEquals(before, verificationCertificates(repository))
  }

  /**
   * An endpoint which is down while the application starts must not take the other login methods
   * with it, so the application comes up and that one registration authenticates nobody. It cannot
   * fall back to anything: with no document there is no verification certificate either.
   */
  @Test
  fun shouldStartWithoutMetadataWhenSourceIsUnreachable() {
    val repository = assertDoesNotThrow { unreachableRepository() }
    Assertions.assertNull(repository.findByRegistrationId(registrationId.toString()))
  }

  /**
   * The point of tolerating it: OpenSAML keeps retrying, and the registration comes up by itself.
   */
  @Test
  fun shouldResolveOnceUnreachableSourceBecomesAvailable() {
    val repository = unreachableRepository()
    Assertions.assertNull(repository.findByRegistrationId(registrationId.toString()))

    checkNotNull(server).status = STATUS_OK
    repository.refreshMetadata()

    Assertions.assertEquals(
        setOf(identityProvider.signingCertificate), verificationCertificates(repository))
  }

  /**
   * Spring's SAML configurer iterates the repository twice while it builds the filter chain, and
   * the metadata endpoint iterates it per request, so an outage must not become a stream of
   * identical errors. Recovery is announced once too, because nothing else says the registration
   * came back.
   */
  @Test
  fun shouldReportUnresolvedRegistrationOnceAndRecoveryOnce() {
    val repository = unreachableRepository()
    repeat(3) { repository.findByRegistrationId(registrationId.toString()) }
    repeat(2) { repository.iterator() }
    Assertions.assertEquals(listOf(1, 0), logCounts())

    checkNotNull(server).status = STATUS_OK
    repository.refreshMetadata()
    repeat(3) { repository.findByRegistrationId(registrationId.toString()) }
    repeat(2) { repository.iterator() }

    Assertions.assertEquals(listOf(1, 1), logCounts())
    Assertions.assertTrue(
        messages(Level.INFO).single().contains("authenticates again"),
        messages(Level.INFO).single())
  }

  /** A path which does not resolve is a misconfiguration, and no retry will ever fix it. */
  @Test
  fun shouldFailStartupWhenMetadataFileIsMissing() {
    assertThrows<IOException> { buildRepository("file:/var/empty/core4-missing-metadata.xml") }
  }

  /** Each resolver holds a timer which has to be cancelled exactly once. */
  @Test
  fun shouldDestroyIdempotently() {
    val repository = createRepository(Saml2MetadataValidators.NONE)
    repository.destroy()
    assertDoesNotThrow { repository.destroy() }
  }

  private fun createRepository(
      validators: Saml2MetadataValidators
  ): RefreshableRelyingPartyRegistrationRepository =
      buildRepository(startServer(validators).url).also { repository = it }

  /** Serves an error until the test lets it serve the document. */
  private fun unreachableRepository(): RefreshableRelyingPartyRegistrationRepository {
    val server = startServer(Saml2MetadataValidators.NONE)
    server.status = STATUS_SERVER_ERROR
    return buildRepository(server.url).also { repository = it }
  }

  private fun buildRepository(metadataUri: String) =
      RefreshableRelyingPartyRegistrationRepository(
          relyingPartyProperties(registrationId, identityProvider, metadataUri))

  private fun startServer(validators: Saml2MetadataValidators): Saml2TestMetadataServer {
    val server = Saml2TestMetadataServer(identityProvider.metadataDocument)
    server.validators = validators
    this.server = server
    return server
  }

  /** How many errors and how many infos the repository logged, in that order. */
  private fun logCounts() = listOf(messages(Level.ERROR).size, messages(Level.INFO).size)

  private fun messages(level: Level) =
      logEvents.list.filter { it.level == level }.map { it.formattedMessage }

  private fun verificationCertificates(
      repository: RefreshableRelyingPartyRegistrationRepository
  ): Set<X509Certificate> =
      checkNotNull(repository.findByRegistrationId(registrationId.toString()))
          .assertingPartyMetadata
          .verificationX509Credentials
          .map { it.certificate }
          .toSet()
}

/**
 * Its own identity provider, so rotating the key here cannot reach the context the other SAML tests
 * share.
 */
@TestConfiguration(proxyBeanMethods = false)
class Saml2KeyRotationTestConfiguration {
  @Bean fun rotationIdentityProvider() = Saml2TestIdentityProvider()

  @Bean
  fun rotationPropertyRegistrar(identityProvider: Saml2TestIdentityProvider) =
      DynamicPropertyRegistrar { registry ->
        registerRelyingParty(registry, identityProvider, ROTATION_REGISTRATION_ID)
      }
}

/**
 * The whole path end to end. Both cases rotate the key themselves, so neither depends on the other
 * having run.
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration::class, Saml2KeyRotationTestConfiguration::class)
class Saml2KeyRotationLoginTests {
  @Autowired lateinit var mockMvc: MockMvc
  @Autowired lateinit var identityProvider: Saml2TestIdentityProvider
  @Autowired lateinit var relyingPartyRegistrations: RelyingPartyRegistrationRepository

  private val registrationId = UUID.fromString(ROTATION_REGISTRATION_ID)

  @Test
  fun shouldRejectResponseSignedWithRotatedKeyBeforeRefresh() {
    identityProvider.rotateSigningKey()
    Assertions.assertNotEquals(HttpStatus.OK.value(), login(ROTATION_REJECTED_USER))
  }

  @Test
  fun shouldAcceptResponseSignedWithRotatedKeyAfterRefresh() {
    identityProvider.rotateSigningKey()
    (relyingPartyRegistrations as RefreshableRelyingPartyRegistrationRepository).refreshMetadata()
    Assertions.assertEquals(HttpStatus.OK.value(), login(ROTATION_ACCEPTED_USER))
  }

  private fun login(user: Saml2TestUser): Int =
      mockMvc
          .perform(
              post("/login/saml2/sso/$registrationId")
                  .param("SAMLResponse", identityProvider.encodedResponse(registrationId, user)))
          .andReturn()
          .response
          .status
}
