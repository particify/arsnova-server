/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user

import java.util.UUID
import net.particify.arsnova.core4.user.sso.OidcProperties
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.boot.context.properties.bind.Binder
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource

private const val PREFIX = "security.oidc"
private val REGISTRATION_ID = UUID.fromString("3b1e7c52-9d4a-4f86-a2c0-5e8d1f7b9a63")

/**
 * A malformed issuer would only fail inside discovery, where it is indistinguishable from a
 * provider which is down and retried as one, so it is rejected while the application starts
 * instead.
 */
class OidcPropertiesTests {
  @Test
  fun shouldBindIssuerAsConfigured() {
    val issuer = "https://idp.example.com/realms/test/"
    Assertions.assertEquals(issuer, bind(issuer).registration.getValue(REGISTRATION_ID).issuerUri)
  }

  @Test
  fun shouldAcceptHttpIssuer() {
    Assertions.assertEquals(
        "http://keycloak:8080/realms/dev",
        registration("http://keycloak:8080/realms/dev").issuerUri)
  }

  @Test
  fun shouldFailBindingForIssuerWithoutScheme() {
    val exception = assertThrows<Exception> { bind("idp.example.com/realms/test") }
    Assertions.assertTrue(rootCauseMessage(exception).contains("idp.example.com/realms/test"))
  }

  @Test
  fun shouldRejectIssuerWithoutScheme() {
    Assertions.assertTrue(rejectionMessage("idp.example.com/realms/test").contains("issuer-uri"))
  }

  @Test
  fun shouldRejectIssuerWithOtherScheme() {
    rejectionMessage("ftp://idp.example.com/realms/test")
  }

  @Test
  fun shouldRejectRelativeIssuer() {
    rejectionMessage("/realms/test")
  }

  @Test
  fun shouldRejectIssuerWithoutHost() {
    rejectionMessage("https:///realms/test")
  }

  @Test
  fun shouldRejectIssuerWithQuery() {
    rejectionMessage("https://idp.example.com/realms/test?tenant=a")
  }

  @Test
  fun shouldRejectBlankIssuer() {
    rejectionMessage(" ")
  }

  @Test
  fun shouldAcceptDefaultScope() {
    Assertions.assertTrue("openid" in registration("https://idp.example.com").scope)
  }

  @Test
  fun shouldRejectScopeWithSpace() {
    val message = scopeRejectionMessage("openid", "read write")
    Assertions.assertTrue(message.contains("read write"))
  }

  @Test
  fun shouldRejectScopeWithQuote() {
    Assertions.assertTrue(scopeRejectionMessage("openid", "a\"b").contains("a\"b"))
  }

  @Test
  fun shouldRejectScopeWithBackslash() {
    Assertions.assertTrue(scopeRejectionMessage("openid", "a\\b").contains("a\\b"))
  }

  @Test
  fun shouldRejectEmptyScope() {
    scopeRejectionMessage("openid", "")
  }

  /** An empty environment variable binds as an empty string rather than leaving it unset. */
  @Test
  fun shouldRejectBlankClientSecret() {
    val message =
        assertThrows<IllegalArgumentException> {
              OidcProperties.Registration(
                  issuerUri = "https://idp.example.com", clientId = "client", clientSecret = " ")
            }
            .message!!
    Assertions.assertTrue(message.contains("client-secret"), message)
  }

  @Test
  fun shouldAcceptPublicClientWithoutSecret() {
    Assertions.assertNull(registration("https://idp.example.com").clientSecret)
  }

  private fun registration(issuer: String) =
      OidcProperties.Registration(issuerUri = issuer, clientId = "client")

  private fun scopeRejectionMessage(vararg scope: String): String =
      assertThrows<IllegalArgumentException> {
            OidcProperties.Registration(
                issuerUri = "https://idp.example.com", clientId = "client", scope = scope.toList())
          }
          .message!!

  private fun rejectionMessage(issuer: String): String =
      assertThrows<IllegalArgumentException> { registration(issuer) }.message!!

  private fun rootCauseMessage(exception: Throwable): String =
      generateSequence(exception) { it.cause }.last().message ?: ""

  private fun bind(issuer: String): OidcProperties {
    val properties =
        mapOf(
            "$PREFIX.registration.$REGISTRATION_ID.issuer-uri" to issuer,
            "$PREFIX.registration.$REGISTRATION_ID.client-id" to "client")
    return Binder(MapConfigurationPropertySource(properties))
        .bind(PREFIX, OidcProperties::class.java)
        .get()
  }
}
