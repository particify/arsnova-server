/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user

import java.util.UUID
import net.particify.arsnova.core4.TestcontainersConfiguration
import net.particify.arsnova.core4.user.internal.UserServiceImpl
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc

/**
 * Drives OpenID Connect logins through the filter chain with an [ExternalLoginHook] in the context.
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(
    TestcontainersConfiguration::class,
    OidcTestConfiguration::class,
    Saml2LoginHookTestConfiguration::class)
class OidcLoginHookTests {
  @Autowired lateinit var mockMvc: MockMvc
  @Autowired lateinit var provider: OidcTestProvider
  @Autowired lateinit var hook: RecordingSaml2LoginHook
  @Autowired lateinit var userService: UserServiceImpl

  private val registrationId = UUID.fromString(OIDC_REGISTRATION_ID)

  @BeforeEach fun resetHook() = hook.reset()

  /** The same answer a refused SAML login gets: the error page naming the hook's error code. */
  @Test
  fun shouldRefuseLoginWithoutProvisioningAccount() {
    hook.refuse = true
    val subject = "oidc-hook-refused-subject"
    val response =
        oidcLogin(mockMvc, provider, OIDC_REGISTRATION_ID, mapOf("sub" to subject)).response
    Assertions.assertEquals(401, response.status)
    Assertions.assertTrue(
        response.contentAsString.contains(REFUSAL_ERROR_CODE), response.contentAsString)
    Assertions.assertNull(userService.loadUserByProviderIdAndExternalId(registrationId, subject))
    Assertions.assertTrue(hook.afterCalls.isEmpty())
  }

  /**
   * Rolled back, and answered with the error page rather than the container's. The code is all the
   * page shows of the failure.
   */
  @Test
  fun shouldAnswerFailedProvisioningWithErrorPage() {
    hook.onAfterProvisioning = { error("Provisioning failed in the test hook.") }
    val subject = "oidc-hook-failed-subject"
    val response =
        oidcLogin(mockMvc, provider, OIDC_REGISTRATION_ID, mapOf("sub" to subject)).response
    Assertions.assertEquals(401, response.status)
    Assertions.assertTrue(
        response.contentAsString.contains("server_error"), response.contentAsString)
    Assertions.assertFalse(response.contentAsString.contains("test hook"))
    Assertions.assertNull(userService.loadUserByProviderIdAndExternalId(registrationId, subject))
  }

  /** An array claim arrives as its elements, a scalar as a list of one. */
  @Test
  fun shouldPassClaimsAsValueLists() {
    val subject = "oidc-hook-claims-subject"
    val claims = mapOf("sub" to subject, "groups" to listOf("staff", "lecturers"))
    oidcLogin(mockMvc, provider, OIDC_REGISTRATION_ID, claims)
    for (call in listOf(hook.beforeCalls.single(), hook.afterCalls.single())) {
      Assertions.assertEquals(registrationId, call.registrationId)
      Assertions.assertEquals(listOf(subject), call.attributes["sub"])
      Assertions.assertEquals(listOf("staff", "lecturers"), call.attributes["groups"])
    }
  }
}
