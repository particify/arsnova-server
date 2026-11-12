/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user

import java.net.URI
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.mock.web.MockHttpSession
import org.springframework.test.context.DynamicPropertyRegistrar
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get

/** Leaves `require-verified-mail-address` at its default and publishes a title and order. */
const val OIDC_REGISTRATION_ID = "5a7c9e1b-3d5f-4a8c-9e2b-4d6f8a0c2e41"

/** Configured with `require-verified-mail-address: false`. */
const val OIDC_UNVERIFIED_REGISTRATION_ID = "6b8d0f2c-4e6a-4b9d-8f3c-5e7a9b1d3f52"

/** Points at an issuer nothing answers for, as a provider which is down while the app starts. */
const val OIDC_DOWN_REGISTRATION_ID = "7c9e1a3d-5f7b-4cae-9a4d-6f8b0c2e4a63"

/** Keys accounts on the claim [OIDC_CUSTOM_ID_CLAIM] instead of `sub`. */
const val OIDC_CUSTOM_ID_REGISTRATION_ID = "8d0a2b4e-6c8d-4e1f-a3b5-7c9d1e3f5a74"

const val OIDC_CUSTOM_ID_CLAIM = "employee_id"

const val OIDC_TITLE = "Example OIDC"
const val OIDC_ORDER = 2

private const val CLIENT_ID = "test-client"
private const val CLIENT_SECRET = "test-secret"
private const val PREFIX = "security.oidc.registration"

@TestConfiguration(proxyBeanMethods = false)
class OidcTestConfiguration {
  @Bean(destroyMethod = "stop") fun oidcTestProvider() = OidcTestProvider(CLIENT_ID, CLIENT_SECRET)

  @Bean
  fun oidcPropertyRegistrar(provider: OidcTestProvider) = DynamicPropertyRegistrar { registry ->
    for (id in
        listOf(
            OIDC_REGISTRATION_ID,
            OIDC_UNVERIFIED_REGISTRATION_ID,
            OIDC_CUSTOM_ID_REGISTRATION_ID)) {
      registry.add("$PREFIX.$id.issuer-uri") { provider.issuer }
      registry.add("$PREFIX.$id.client-id") { CLIENT_ID }
      registry.add("$PREFIX.$id.client-secret") { CLIENT_SECRET }
    }
    registry.add("$PREFIX.$OIDC_REGISTRATION_ID.title") { OIDC_TITLE }
    registry.add("$PREFIX.$OIDC_REGISTRATION_ID.order") { OIDC_ORDER }
    registry.add("$PREFIX.$OIDC_UNVERIFIED_REGISTRATION_ID.require-verified-mail-address") { false }
    registry.add("$PREFIX.$OIDC_CUSTOM_ID_REGISTRATION_ID.claim-mapping.id") {
      OIDC_CUSTOM_ID_CLAIM
    }
    registry.add("$PREFIX.$OIDC_DOWN_REGISTRATION_ID.issuer-uri") {
      "http://localhost:1/realms/down"
    }
    registry.add("$PREFIX.$OIDC_DOWN_REGISTRATION_ID.client-id") { CLIENT_ID }
  }
}

/**
 * Runs a whole login: starts it, lets [provider] issue [claims] for the authorization request the
 * application redirected to, and returns the result of the callback.
 */
fun oidcLogin(
    mockMvc: MockMvc,
    provider: OidcTestProvider,
    registrationId: String,
    claims: Map<String, Any>
): MvcResult {
  val login = startOidcLogin(mockMvc, registrationId)
  val code = provider.authorize(login.authorizationUrl, claims)
  return mockMvc
      .perform(
          get("/login/oauth2/code/$registrationId")
              .param("code", code)
              .param("state", login.state)
              .session(login.session))
      .andReturn()
}

/**
 * Starts a login and returns the result of a callback answering it with the [error] code, as a
 * provider redirects back when it refuses the request.
 */
fun oidcErrorCallback(mockMvc: MockMvc, registrationId: String, error: String): MvcResult {
  val login = startOidcLogin(mockMvc, registrationId)
  return mockMvc
      .perform(
          get("/login/oauth2/code/$registrationId")
              .param("error", error)
              .param("state", login.state)
              .session(login.session))
      .andReturn()
}

private class StartedOidcLogin(
    val session: MockHttpSession,
    val authorizationUrl: String,
    val state: String
)

private fun startOidcLogin(mockMvc: MockMvc, registrationId: String): StartedOidcLogin {
  val session = MockHttpSession()
  val authorizationUrl =
      checkNotNull(
          mockMvc
              .perform(get("/oauth2/authorization/$registrationId").session(session))
              .andReturn()
              .response
              .redirectedUrl) {
            "The login did not redirect to the provider."
          }
  val state =
      URI.create(authorizationUrl)
          .query
          .split("&")
          .single { it.startsWith("state=") }
          .substringAfter("=")
  return StartedOidcLogin(session, authorizationUrl, state)
}
