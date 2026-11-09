/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.security.internal

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import net.particify.arsnova.core4.user.OidcTestProvider
import net.particify.arsnova.core4.user.sso.OidcProperties
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.core.ClientAuthenticationMethod

private val REGISTRATION_ID = UUID.fromString("8d0f2b4e-6a8c-4dbf-8b5e-7a9c1d3f5b74")
private val RETRY_DELAY = Duration.ofSeconds(30)

class OidcClientRegistrationRepositoryTests {
  private val provider = OidcTestProvider("client", "secret")
  private var now = Instant.parse("2026-01-01T00:00:00Z")
  private val clock =
      object : Clock() {
        override fun getZone() = ZoneOffset.UTC

        override fun withZone(zone: java.time.ZoneId?) = this

        override fun instant() = now
      }

  @AfterEach fun stopProvider() = provider.stop()

  @Test
  fun shouldReadDiscoveryDocumentOnceOnFirstLookup() {
    val repository = repository()
    Assertions.assertEquals(0, provider.discoveryRequestCount)
    val registration = checkNotNull(repository.findByRegistrationId(REGISTRATION_ID.toString()))
    repository.findByRegistrationId(REGISTRATION_ID.toString())
    Assertions.assertEquals(1, provider.discoveryRequestCount)
    Assertions.assertEquals("${provider.issuer}/token", registration.providerDetails.tokenUri)
    Assertions.assertTrue(registration.clientSettings.isRequireProofKey)
  }

  @Test
  fun shouldRetryUnreachableProviderOnlyAfterDelay() {
    provider.available = false
    val repository = repository()
    Assertions.assertNull(repository.findByRegistrationId(REGISTRATION_ID.toString()))
    provider.available = true
    Assertions.assertNull(repository.findByRegistrationId(REGISTRATION_ID.toString()))
    Assertions.assertEquals(1, provider.discoveryRequestCount)

    now = now.plus(RETRY_DELAY)

    Assertions.assertNotNull(repository.findByRegistrationId(REGISTRATION_ID.toString()))
  }

  @Test
  fun shouldRetryProviderServingInvalidMetadataOnlyAfterDelay() {
    provider.servesValidMetadata = false
    val repository = repository()
    Assertions.assertNull(repository.findByRegistrationId(REGISTRATION_ID.toString()))
    provider.servesValidMetadata = true
    Assertions.assertNull(repository.findByRegistrationId(REGISTRATION_ID.toString()))
    Assertions.assertEquals(1, provider.discoveryRequestCount)

    now = now.plus(RETRY_DELAY)

    Assertions.assertNotNull(repository.findByRegistrationId(REGISTRATION_ID.toString()))
  }

  @Test
  fun shouldRejectDocumentNamingAnotherIssuer() {
    val repository = repository(issuer = provider.issuer + "/")
    Assertions.assertNull(repository.findByRegistrationId(REGISTRATION_ID.toString()))
  }

  @Test
  fun shouldAuthenticatePublicClientWithoutSecret() {
    val registration =
        checkNotNull(
            repository(clientSecret = null).findByRegistrationId(REGISTRATION_ID.toString()))
    Assertions.assertEquals(
        ClientAuthenticationMethod.NONE, registration.clientAuthenticationMethod)
  }

  @Test
  fun shouldNotKnowUnconfiguredRegistration() {
    Assertions.assertNull(repository().findByRegistrationId(UUID.randomUUID().toString()))
  }

  private fun repository(issuer: String = provider.issuer, clientSecret: String? = "secret") =
      OidcClientRegistrationRepository(
          OidcProperties(
              mapOf(
                  REGISTRATION_ID to
                      OidcProperties.Registration(
                          issuerUri = issuer, clientId = "client", clientSecret = clientSecret))),
          RETRY_DELAY,
          clock)
}
