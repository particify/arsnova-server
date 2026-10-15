/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user

import java.util.UUID
import net.particify.arsnova.core4.TestcontainersConfiguration
import net.particify.arsnova.core4.user.internal.UserServiceImpl
import net.particify.arsnova.core4.user.sso.Saml2LoginHook
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.security.saml2.core.Saml2Error
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationException
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate

private const val REFUSAL_ERROR_CODE = "test_refusal"

/** A role from the predefined data which no account is given by provisioning itself. */
private const val HOOK_ROLE = "ADMIN"

private val SAML_REFUSED_USER =
    Saml2TestUser(subjectId = "saml-hook-refused-subject", mailAddress = "saml.refused@example.com")

private val SAML_HOOK_ROLE_USER =
    Saml2TestUser(subjectId = "saml-hook-role-subject", mailAddress = "saml.hook.role@example.com")

/**
 * Registers the test hook. It is kept apart from [Saml2TestConfiguration] on purpose: the other
 * SAML login tests have to run with no hook in the context at all.
 */
@TestConfiguration(proxyBeanMethods = false)
class Saml2LoginHookTestConfiguration {
  @Bean fun recordingSaml2LoginHook() = RecordingSaml2LoginHook()
}

/** Records every call and applies whatever a test has armed it with. */
class RecordingSaml2LoginHook : Saml2LoginHook {
  data class Call(val registrationId: UUID, val attributes: Map<String, List<Any>>)

  val beforeCalls = mutableListOf<Call>()
  val afterCalls = mutableListOf<Call>()
  var refuse = false
  var onAfterProvisioning: (User) -> Unit = {}

  override fun beforeProvisioning(registrationId: UUID, attributes: Map<String, List<Any>>) {
    beforeCalls += Call(registrationId, attributes)
    if (refuse) {
      throw Saml2AuthenticationException(Saml2Error(REFUSAL_ERROR_CODE, "Refused by test hook."))
    }
  }

  override fun afterProvisioning(
      user: User,
      registrationId: UUID,
      attributes: Map<String, List<Any>>
  ) {
    afterCalls += Call(registrationId, attributes)
    onAfterProvisioning(user)
  }

  fun reset() {
    beforeCalls.clear()
    afterCalls.clear()
    refuse = false
    onAfterProvisioning = {}
  }
}

/** Drives SAML logins through the filter chain with a [Saml2LoginHook] in the context. */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(
    TestcontainersConfiguration::class,
    Saml2TestConfiguration::class,
    Saml2LoginHookTestConfiguration::class)
class Saml2LoginHookTests {
  @Autowired lateinit var mockMvc: MockMvc
  @Autowired lateinit var identityProvider: Saml2TestIdentityProvider
  @Autowired lateinit var hook: RecordingSaml2LoginHook
  @Autowired lateinit var userService: UserServiceImpl
  @Autowired lateinit var transactionManager: PlatformTransactionManager

  private val registrationId = UUID.fromString(SAML_MAIL_REGISTRATION_ID)

  @BeforeEach fun resetHook() = hook.reset()

  @Test
  fun shouldRefuseLoginWithoutProvisioningAccount() {
    hook.refuse = true
    val page =
        login(SAML_REFUSED_USER)
            .andExpect(status().isUnauthorized())
            .andReturn()
            .response
            .contentAsString
    Assertions.assertTrue(page.contains(REFUSAL_ERROR_CODE), page)
    Assertions.assertNull(
        userService.loadUserByProviderIdAndExternalId(registrationId, SAML_REFUSED_USER.subjectId))
    Assertions.assertTrue(hook.afterCalls.isEmpty())
  }

  @Test
  fun shouldPassRegistrationAndAssertedAttributes() {
    login(SAML_HOOK_ROLE_USER).andExpect(status().isOk())
    val expected = SAML_HOOK_ROLE_USER.attributes.mapValues { listOf(it.value) }
    for (call in listOf(hook.beforeCalls.single(), hook.afterCalls.single())) {
      Assertions.assertEquals(registrationId, call.registrationId)
      Assertions.assertEquals(expected, call.attributes)
    }
  }

  /** The first login creates the account and the second one updates it. */
  @Test
  fun shouldPersistRoleChangesOnCreateAndUpdate() {
    hook.onAfterProvisioning = { it.roles += userService.findRoleByName(HOOK_ROLE) }
    login(SAML_HOOK_ROLE_USER).andExpect(status().isOk())
    Assertions.assertTrue(roleNames(SAML_HOOK_ROLE_USER).contains(HOOK_ROLE))

    hook.onAfterProvisioning = { user -> user.roles.removeIf { it.name == HOOK_ROLE } }
    login(SAML_HOOK_ROLE_USER).andExpect(status().isOk())
    Assertions.assertFalse(roleNames(SAML_HOOK_ROLE_USER).contains(HOOK_ROLE))
  }

  private fun login(user: Saml2TestUser): ResultActions =
      mockMvc.perform(
          post("/login/saml2/sso/$registrationId")
              .param("SAMLResponse", identityProvider.encodedResponse(registrationId, user)))

  /** A reloaded account is detached, so its roles are only reachable inside a transaction. */
  private fun roleNames(user: Saml2TestUser): Set<String?> =
      checkNotNull(
          TransactionTemplate(transactionManager).execute { _ ->
            val account =
                checkNotNull(
                    userService.loadUserByProviderIdAndExternalId(registrationId, user.subjectId))
            account.roles.map { it.name }.toSet()
          })
}
