/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user

import java.time.Duration
import java.util.Locale
import java.util.UUID
import net.particify.arsnova.core4.TestcontainersConfiguration
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.graphql.ExecutionGraphQlResponse
import org.springframework.graphql.ExecutionGraphQlService
import org.springframework.graphql.execution.ErrorType
import org.springframework.graphql.support.DefaultExecutionGraphQlRequest
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.ActiveProfiles

private val TIMEOUT = Duration.ofSeconds(10)

/** Made of the names below, so it is strong unless the estimate knows them. */
private const val NAME_PASSWORD = "QuorvexBlandimor"
private const val GIVEN_NAME = "Quorvex"
private const val SURNAME = "Blandimor"

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration::class)
class UserPasswordStrengthQueryTests {
  @Autowired lateinit var graphQlService: ExecutionGraphQlService

  @AfterEach
  fun clearSecurityContext() {
    SecurityContextHolder.clearContext()
  }

  @Test
  fun shouldRejectAnonymousCaller() {
    val response = query(NAME_PASSWORD)
    Assertions.assertEquals(ErrorType.UNAUTHORIZED, response.errors.single().errorType)
  }

  @Test
  fun shouldEstimateForGuest() {
    authenticate(User(id = UUID.randomUUID()))
    Assertions.assertEquals(
        mapOf("acceptable" to true, "relativeStrength" to 1.0, "hint" to null),
        strength(NAME_PASSWORD))
  }

  /** Without an account, only the passed mail address is known about the password's owner. */
  @Test
  fun shouldEstimateForReusableChallengeToken() {
    SecurityContextHolder.getContext().authentication = reusableChallengeAuthentication()
    val password = "${SURNAME}2024!x"
    Assertions.assertEquals(true, strength(password)["acceptable"])
    Assertions.assertEquals(
        false, strength(password, "quorvex.blandimor@example.org")["acceptable"])
  }

  /** Its role is not the account's, so the resolvers which require an account deny it. */
  @Test
  fun shouldDenyUserResolversToReusableChallengeToken() {
    SecurityContextHolder.getContext().authentication = reusableChallengeAuthentication()
    val response = execute("{ currentUser { id } }")
    Assertions.assertEquals(ErrorType.FORBIDDEN, response.errors.single().errorType)
  }

  @Test
  fun shouldUseNamesOfCaller() {
    authenticate(User(id = UUID.randomUUID(), givenName = GIVEN_NAME, surname = SURNAME))
    Assertions.assertEquals(false, strength(NAME_PASSWORD)["acceptable"])
  }

  @Test
  fun shouldUsePassedMailAddress() {
    authenticate(User(id = UUID.randomUUID()))
    val password = "${SURNAME}2024!x"
    Assertions.assertEquals(true, strength(password)["acceptable"])
    Assertions.assertEquals(
        false, strength(password, "quorvex.blandimor@example.org")["acceptable"])
  }

  /** Rejected as when it is set, so the client does not rate a password it cannot submit. */
  @Test
  fun shouldReportInvalidPasswordWithErrorCode() {
    authenticate(User(id = UUID.randomUUID()))
    @Suppress("UNCHECKED_CAST")
    val error = (query("$NAME_PASSWORD ").toMap()["errors"] as List<Map<String, Any>>).single()
    Assertions.assertEquals(
        mapOf("classification" to "BAD_REQUEST", "code" to "INVALID_PASSWORD"), error["extensions"])
  }

  @Test
  fun shouldLocalizeHintByRequestLocale() {
    authenticate(User(id = UUID.randomUUID()))
    Assertions.assertEquals(
        "Das ist sehr ähnlich zu einem häufig verwendeten Passwort.",
        strength("Passwort123", locale = Locale.GERMAN)["hint"])
  }

  /** As a reusable challenge token authenticates: without an account, with only its role. */
  private fun reusableChallengeAuthentication() =
      object :
          AbstractAuthenticationToken(
              listOf(SimpleGrantedAuthority("ROLE_CHALLENGE_SOLVED_REUSABLE"))) {
        init {
          isAuthenticated = true
        }

        override fun getCredentials() = null

        override fun getPrincipal() = null
      }

  private fun authenticate(user: User) {
    SecurityContextHolder.getContext().authentication =
        UsernamePasswordAuthenticationToken.authenticated(
            user, null, listOf(SimpleGrantedAuthority("ROLE_USER")))
  }

  @Suppress("UNCHECKED_CAST")
  private fun strength(
      password: String,
      mailAddress: String? = null,
      locale: Locale = Locale.ENGLISH
  ): Map<String, Any?> {
    val response = query(password, mailAddress, locale)
    Assertions.assertTrue(response.errors.isEmpty(), response.errors.toString())
    return response.getData<Map<String, Any?>>()!!["userPasswordStrength"] as Map<String, Any?>
  }

  private fun query(
      password: String,
      mailAddress: String? = null,
      locale: Locale = Locale.ENGLISH
  ): ExecutionGraphQlResponse {
    val document =
        """
        query (${'$'}query: UserPasswordStrengthQueryInput!) {
          userPasswordStrength(query: ${'$'}query) { acceptable relativeStrength hint }
        }
        """
    val variables = mapOf("query" to mapOf("password" to password, "mailAddress" to mailAddress))
    return execute(document, variables, locale)
  }

  private fun execute(
      document: String,
      variables: Map<String, Any?> = mapOf(),
      locale: Locale = Locale.ENGLISH
  ): ExecutionGraphQlResponse {
    val request =
        DefaultExecutionGraphQlRequest(
            document, null, variables, null, UUID.randomUUID().toString(), locale)
    val securityContext = SecurityContextHolder.getContext()
    return graphQlService
        .execute(request)
        .contextWrite { it.put(SecurityContext::class.java.name, securityContext) }
        .block(TIMEOUT)!!
  }
}
