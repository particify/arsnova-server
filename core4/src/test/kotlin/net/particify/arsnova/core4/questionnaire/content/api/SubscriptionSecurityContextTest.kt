/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.content.api

import graphql.ExecutionResult
import graphql.schema.DataFetcher
import java.io.Serializable
import java.time.Duration
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference
import net.particify.arsnova.core4.questionnaire.content.ChoiceContentFormatData
import net.particify.arsnova.core4.questionnaire.content.Content
import net.particify.arsnova.core4.questionnaire.content.SolutionVisibility
import net.particify.arsnova.core4.questionnaire.questionnaire.Questionnaire
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.core.io.ByteArrayResource
import org.springframework.graphql.execution.DefaultExecutionGraphQlService
import org.springframework.graphql.execution.GraphQlSource
import org.springframework.graphql.support.DefaultExecutionGraphQlRequest
import org.springframework.security.access.PermissionEvaluator
import org.springframework.security.authentication.TestingAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import reactor.core.publisher.Flux
import reactor.core.scheduler.Schedulers

private const val SCHEMA =
    """
    type Query { ping: String }
    type Subscription { contentUpdated: Content }
    type Content { formatData: ChoiceFormatData }
    type ChoiceFormatData { correctOptionIndexes: [Int!] }
    """
private const val SUBSCRIPTION =
    "subscription { contentUpdated { formatData { correctOptionIndexes } } }"
private val TIMEOUT = Duration.ofSeconds(10)

/**
 * Pins the assumption the withholding relies on: a subscription item is emitted on a thread without
 * the subscriber's security context, yet its field resolvers still run in that context, because
 * Spring for GraphQL restores the context the transport captured when the subscription started.
 */
class SubscriptionSecurityContextTest {
  private val permissionEvaluator = Mockito.mock(PermissionEvaluator::class.java)
  private val controller = ContentFormatDataController(SolutionVisibility(permissionEvaluator))
  private val emissionAuthentication = AtomicReference<Authentication?>()
  private val content =
      Content(
          id = UUID.randomUUID(),
          questionnaire = Questionnaire(id = UUID.randomUUID(), correctOptionsPublished = false),
          formatData = ChoiceContentFormatData(listOf("A", "B"), listOf(1), false))

  init {
    Mockito.`when`(
            permissionEvaluator.hasPermission(
                Mockito.any(Authentication::class.java),
                Mockito.any(Serializable::class.java),
                Mockito.anyString(),
                Mockito.any()))
        .thenAnswer { (it.arguments[0] as Authentication).name == "editor" }
  }

  @AfterEach
  fun clearAuthentication() {
    SecurityContextHolder.clearContext()
  }

  private fun subscribeAs(user: String): Any? {
    val contentUpdated = DataFetcher {
      Flux.just(content).publishOn(Schedulers.boundedElastic()).doOnNext {
        emissionAuthentication.set(SecurityContextHolder.getContext().authentication)
      }
    }
    val formatData = DataFetcher { controller.formatData(it.getSource<Content>()!!) }
    val graphQlSource =
        GraphQlSource.schemaResourceBuilder()
            .schemaResources(ByteArrayResource(SCHEMA.toByteArray()))
            .configureRuntimeWiring { wiring ->
              wiring
                  .type("Subscription") { it.dataFetcher("contentUpdated", contentUpdated) }
                  .type("Content") { it.dataFetcher("formatData", formatData) }
            }
            .build()
    SecurityContextHolder.getContext().authentication =
        TestingAuthenticationToken(user, null, "ROLE_USER")
    // The transports put the request's security context into the Reactor context under the key
    // of Spring for GraphQL's SecurityContextThreadLocalAccessor, as done here.
    val securityContext = SecurityContextHolder.getContext()
    val response =
        DefaultExecutionGraphQlService(graphQlSource)
            .execute(DefaultExecutionGraphQlRequest(SUBSCRIPTION, null, null, null, "1", null))
            .contextWrite { it.put(SecurityContext::class.java.name, securityContext) }
            .block(TIMEOUT)!!
    SecurityContextHolder.clearContext()
    val result =
        Flux.from(response.getData<org.reactivestreams.Publisher<ExecutionResult>>()!!)
            .blockFirst(TIMEOUT)!!
    Assertions.assertNull(emissionAuthentication.get(), "emitted with the subscriber's context")
    val data = result.getData<Map<String, Map<String, Map<String, Any?>>>>()
    return data["contentUpdated"]!!["formatData"]!!["correctOptionIndexes"]
  }

  @Test
  fun shouldResolveFieldsOfSubscriptionItemsInTheSubscribersContext() {
    Assertions.assertEquals(listOf(1), subscribeAs("editor"))
  }

  @Test
  fun shouldWithholdTheSolutionPerSubscriber() {
    Assertions.assertNull(subscribeAs("participant"))
  }
}
