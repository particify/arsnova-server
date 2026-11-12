/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user

import java.util.UUID
import net.particify.arsnova.core4.TestcontainersConfiguration
import net.particify.arsnova.core4.security.REFRESH_TOKEN_COOKIE
import net.particify.arsnova.core4.system.compat.LegacyConfigurationController.LegacyConfiguration
import net.particify.arsnova.core4.system.compat.LegacyConfigurationController.LegacyConfiguration.LegacyAuthenticationProvider
import net.particify.arsnova.core4.user.internal.UserServiceImpl
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.json.JsonMapper

/**
 * Drives OpenID Connect logins through Spring Security's filter chain against an in-process
 * provider, which signs the ID token and insists on the client secret and PKCE.
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration::class, OidcTestConfiguration::class)
class OidcLoginHttpTests {
  @Autowired lateinit var mockMvc: MockMvc
  @Autowired lateinit var provider: OidcTestProvider
  @Autowired lateinit var userService: UserServiceImpl
  @Autowired lateinit var jsonMapper: JsonMapper

  @Test
  fun shouldCreateAccountOnFirstLoginAndUpdateItOnTheNext() {
    val subject = "oidc-create-subject"
    val address = "oidc.create@example.com"
    val first = login(OIDC_REGISTRATION_ID, claims(subject, address, givenName = "Ingrid"))
    Assertions.assertEquals(200, first.response.status, first.response.contentAsString)
    Assertions.assertNotNull(refreshCookie(first))
    val created = loadUser(OIDC_REGISTRATION_ID, subject)
    Assertions.assertEquals(address, created.username)
    Assertions.assertEquals(address, created.mailAddress)
    Assertions.assertEquals("Ingrid", created.givenName)

    login(OIDC_REGISTRATION_ID, claims(subject, address, givenName = "Ingeborg"))

    val updated = loadUser(OIDC_REGISTRATION_ID, subject)
    Assertions.assertEquals(created.id, updated.id)
    Assertions.assertEquals("Ingeborg", updated.givenName)
  }

  /**
   * An unverified address must not be imported, linked on, or become the username; the account is
   * still created and left unverified.
   */
  @Test
  fun shouldIgnoreUnverifiedMailAddressByDefault() {
    val subject = "oidc-unverified-subject"
    val result =
        login(
            OIDC_REGISTRATION_ID, claims(subject, "oidc.unverified@example.com", verified = false))
    Assertions.assertEquals(200, result.response.status)
    val user = loadUser(OIDC_REGISTRATION_ID, subject)
    Assertions.assertNull(user.mailAddress)
    Assertions.assertNull(user.username)
  }

  /** A provider which never sends `email_verified` at all is treated the same way. */
  @Test
  fun shouldIgnoreMailAddressWithoutVerificationClaim() {
    val subject = "oidc-no-verification-subject"
    val claims = claims(subject, "oidc.no.verification@example.com") - "email_verified"
    login(OIDC_REGISTRATION_ID, claims)
    Assertions.assertNull(loadUser(OIDC_REGISTRATION_ID, subject).mailAddress)
  }

  @Test
  fun shouldImportUnverifiedMailAddressWhenNotRequired() {
    val subject = "oidc-unverified-accepted-subject"
    val address = "oidc.unverified.accepted@example.com"
    login(OIDC_UNVERIFIED_REGISTRATION_ID, claims(subject, address, verified = false))
    val user = loadUser(OIDC_UNVERIFIED_REGISTRATION_ID, subject)
    Assertions.assertEquals(address, user.mailAddress)
    Assertions.assertEquals(address, user.username)
  }

  /** The application has started although the provider could not be asked, and fails the login. */
  @Test
  fun shouldAnswerLoginViaUnreachableProviderWithErrorPage() {
    val response =
        mockMvc
            .perform(get("/oauth2/authorization/$OIDC_DOWN_REGISTRATION_ID"))
            .andExpect(status().isUnauthorized())
            .andReturn()
            .response
    Assertions.assertTrue(response.contentAsString.contains("Login via single sign-on failed."))
  }

  @Test
  fun shouldShowProviderErrorCode() {
    val response = oidcErrorCallback(mockMvc, OIDC_REGISTRATION_ID, "access_denied").response
    Assertions.assertEquals(401, response.status)
    Assertions.assertTrue(response.contentAsString.contains("access_denied"))
  }

  /** The code is the provider's request parameter, which anyone can fill in. */
  @Test
  fun shouldNotShowErrorCodeOutsideCharset() {
    val response = oidcErrorCallback(mockMvc, OIDC_REGISTRATION_ID, "a\r\nb").response
    Assertions.assertEquals(401, response.status)
    Assertions.assertFalse(response.contentAsString.contains("a\r\nb"))
    Assertions.assertTrue(response.contentAsString.contains("invalid_error"))
  }

  @Test
  fun shouldKeyAccountOnConfiguredIdClaim() {
    val employeeId = "oidc-custom-id-employee"
    val claims = claims("oidc-custom-id-subject", "oidc.custom.id@example.com")
    val result =
        login(OIDC_CUSTOM_ID_REGISTRATION_ID, claims + (OIDC_CUSTOM_ID_CLAIM to employeeId))
    Assertions.assertEquals(200, result.response.status)
    loadUser(OIDC_CUSTOM_ID_REGISTRATION_ID, employeeId)
  }

  @Test
  fun shouldFailLoginWithoutConfiguredIdClaim() {
    val claims = claims("oidc-missing-id-subject", "oidc.missing.id@example.com")
    val response = login(OIDC_CUSTOM_ID_REGISTRATION_ID, claims).response
    Assertions.assertEquals(401, response.status)
    Assertions.assertTrue(response.contentAsString.contains("missing_id_claim"))
  }

  @Test
  fun shouldDispatchSsoRouteToOidcLogin() {
    mockMvc
        .perform(get("/auth/sso/$OIDC_REGISTRATION_ID"))
        .andExpect(redirectedUrl("/oauth2/authorization/$OIDC_REGISTRATION_ID"))
    mockMvc.perform(get("/auth/sso/${UUID.randomUUID()}")).andExpect(status().isNotFound())
  }

  @Test
  fun shouldPublishRegistrationAsSsoProvider() {
    val provider = configurationProvider(OIDC_REGISTRATION_ID)
    Assertions.assertEquals(LegacyAuthenticationProvider.Type.SSO, provider.type)
    Assertions.assertEquals(OIDC_TITLE, provider.title)
    Assertions.assertEquals(OIDC_ORDER, provider.order)
    Assertions.assertEquals("OIDC", configurationProvider(OIDC_UNVERIFIED_REGISTRATION_ID).title)
  }

  private fun login(registrationId: String, claims: Map<String, Any>): MvcResult =
      oidcLogin(mockMvc, provider, registrationId, claims)

  private fun claims(
      subject: String,
      mailAddress: String,
      givenName: String = "Given",
      verified: Boolean = true
  ): Map<String, Any> =
      mapOf(
          "sub" to subject,
          "email" to mailAddress,
          "email_verified" to verified,
          "given_name" to givenName,
          "family_name" to "Family")

  private fun refreshCookie(result: MvcResult): String? =
      result.response.getHeaders(HttpHeaders.SET_COOKIE).singleOrNull {
        it.startsWith("$REFRESH_TOKEN_COOKIE=")
      }

  private fun configurationProvider(id: String): LegacyAuthenticationProvider {
    val response = mockMvc.perform(get("/configuration")).andReturn().response.contentAsString
    return jsonMapper
        .readValue(response, LegacyConfiguration::class.java)
        .authenticationProviders
        .single { it.id == id }
  }

  private fun loadUser(registrationId: String, subject: String): User =
      checkNotNull(
          userService.loadUserByProviderIdAndExternalId(UUID.fromString(registrationId), subject)) {
            "No account for $subject"
          }
}
