/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.security

import java.security.cert.X509Certificate
import java.util.UUID
import net.particify.arsnova.core4.TestcontainersConfiguration
import net.particify.arsnova.core4.user.Saml2MetadataValidators
import net.particify.arsnova.core4.user.Saml2TestIdentityProvider
import net.particify.arsnova.core4.user.Saml2TestMetadataServer
import net.particify.arsnova.core4.user.Saml2TestUser
import net.particify.arsnova.core4.user.registerRelyingParty
import net.particify.arsnova.core4.user.relyingPartyProperties
import net.shibboleth.shared.component.ComponentInitializationException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
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
private const val STATUS_SERVER_ERROR = 500

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

  @BeforeEach
  fun startIdentityProvider() {
    identityProvider = Saml2TestIdentityProvider()
  }

  @AfterEach
  fun releaseResources() {
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
   * Pins the fail-fast decision: a registration which can never work is worse than a service which
   * refuses to start.
   */
  @Test
  fun shouldFailStartupWhenMetadataIsUnreachable() {
    val server = startServer(Saml2MetadataValidators.NONE)
    server.status = STATUS_SERVER_ERROR
    assertThrows<ComponentInitializationException> { buildRepository(server.url) }
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

  private fun buildRepository(metadataUri: String) =
      RefreshableRelyingPartyRegistrationRepository(
          relyingPartyProperties(registrationId, identityProvider, metadataUri))

  private fun startServer(validators: Saml2MetadataValidators): Saml2TestMetadataServer {
    val server = Saml2TestMetadataServer(identityProvider.metadataDocument)
    server.validators = validators
    this.server = server
    return server
  }

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
