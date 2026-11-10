/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.common

import java.time.Duration
import java.util.Locale
import java.util.UUID
import net.particify.arsnova.core4.TestcontainersConfiguration
import net.particify.arsnova.core4.user.User
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.ActiveProfiles

private val TIMEOUT = Duration.ofSeconds(10)
private const val DOCUMENT = "{ languages { code nativeName } }"

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration::class)
class LanguageQueryTests {
  @Autowired lateinit var graphQlService: ExecutionGraphQlService

  @AfterEach
  fun clearSecurityContext() {
    SecurityContextHolder.clearContext()
  }

  @Test
  fun shouldRejectAnonymousCaller() {
    Assertions.assertEquals(ErrorType.UNAUTHORIZED, execute().errors.single().errorType)
  }

  @Test
  fun shouldListValidatedCodesSortedByCode() {
    authenticate()
    val codes = languages().map { it["code"] }
    Assertions.assertEquals(LANGUAGE_ISO_639_CODES.toList(), codes)
    Assertions.assertEquals(codes.sortedBy { it }, codes)
  }

  @Test
  fun shouldExcludeObsoleteCodes() {
    authenticate()
    val codes = languages().map { it["code"] }
    Assertions.assertTrue(codes.none { it in listOf("iw", "in", "ji") })
    Assertions.assertTrue(codes.containsAll(listOf("he", "id", "yi")))
  }

  @Test
  fun shouldNameLanguagesInThemselves() {
    authenticate()
    val names = languages().associate { it["code"] to it["nativeName"] }
    Assertions.assertEquals("Deutsch", names["de"])
    Assertions.assertEquals("English", names["en"])
    Assertions.assertEquals("français", names["fr"])
  }

  private fun authenticate() {
    SecurityContextHolder.getContext().authentication =
        UsernamePasswordAuthenticationToken.authenticated(
            User(id = UUID.randomUUID()), null, listOf(SimpleGrantedAuthority("ROLE_USER")))
  }

  @Suppress("UNCHECKED_CAST")
  private fun languages(): List<Map<String, String>> {
    val response = execute()
    Assertions.assertTrue(response.errors.isEmpty(), response.errors.toString())
    return response.getData<Map<String, Any>>()!!["languages"] as List<Map<String, String>>
  }

  private fun execute(): ExecutionGraphQlResponse {
    val request =
        DefaultExecutionGraphQlRequest(
            DOCUMENT, null, mapOf(), null, UUID.randomUUID().toString(), Locale.ENGLISH)
    val securityContext = SecurityContextHolder.getContext()
    return graphQlService
        .execute(request)
        .contextWrite { it.put(SecurityContext::class.java.name, securityContext) }
        .block(TIMEOUT)!!
  }
}
