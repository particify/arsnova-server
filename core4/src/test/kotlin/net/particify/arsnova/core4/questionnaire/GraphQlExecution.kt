/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire

import java.time.Duration
import java.util.UUID
import org.springframework.graphql.ExecutionGraphQlResponse
import org.springframework.graphql.ExecutionGraphQlService
import org.springframework.graphql.support.DefaultExecutionGraphQlRequest
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder

private val TIMEOUT = Duration.ofSeconds(10)

/**
 * Executes [document] against the application's schema and controllers in the security context of
 * the calling thread, which the transports otherwise hand over in the Reactor context.
 */
fun ExecutionGraphQlService.executeAsCurrentUser(document: String): ExecutionGraphQlResponse {
  val securityContext = SecurityContextHolder.getContext()
  val request =
      DefaultExecutionGraphQlRequest(document, null, null, null, UUID.randomUUID().toString(), null)
  return execute(request)
      .contextWrite { it.put(SecurityContext::class.java.name, securityContext) }
      .block(TIMEOUT)!!
}
