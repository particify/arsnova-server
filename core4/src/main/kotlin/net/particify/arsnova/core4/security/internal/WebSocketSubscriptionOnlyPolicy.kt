/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.security.internal

import graphql.ExecutionResult
import graphql.GraphqlErrorBuilder
import graphql.execution.AbortExecutionException
import graphql.execution.instrumentation.Instrumentation
import graphql.execution.instrumentation.InstrumentationContext
import graphql.execution.instrumentation.InstrumentationState
import graphql.execution.instrumentation.parameters.InstrumentationExecuteOperationParameters
import graphql.language.OperationDefinition
import org.springframework.graphql.execution.ErrorType
import org.springframework.graphql.server.WebGraphQlInterceptor
import org.springframework.graphql.server.WebGraphQlRequest
import org.springframework.graphql.server.WebGraphQlResponse
import org.springframework.graphql.server.WebSocketGraphQlRequest
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

private const val WEBSOCKET_REQUEST_KEY = "net.particify.arsnova.core4.websocketRequest"

/**
 * Restricts the WebSocket transport to subscriptions, the only operations clients use it for.
 *
 * The interceptor marks the request; the operation type is checked once graphql-java has selected
 * the operation, so `operationName` is honoured.
 */
@Component
class WebSocketSubscriptionOnlyPolicy : WebGraphQlInterceptor, Instrumentation {
  override fun intercept(
      request: WebGraphQlRequest,
      chain: WebGraphQlInterceptor.Chain
  ): Mono<WebGraphQlResponse> {
    if (request is WebSocketGraphQlRequest) {
      request.configureExecutionInput { _, builder ->
        builder.graphQLContext { it.put(WEBSOCKET_REQUEST_KEY, true) }.build()
      }
    }
    return chain.next(request)
  }

  override fun beginExecuteOperation(
      parameters: InstrumentationExecuteOperationParameters,
      state: InstrumentationState?
  ): InstrumentationContext<ExecutionResult>? {
    val context = parameters.executionContext
    val operation = context.operationDefinition.operation
    if (context.graphQLContext.getOrDefault(WEBSOCKET_REQUEST_KEY, false) &&
        operation != OperationDefinition.Operation.SUBSCRIPTION) {
      val error =
          GraphqlErrorBuilder.newError()
              .errorType(ErrorType.BAD_REQUEST)
              .message(
                  "Only subscriptions are supported over WebSocket, not a " +
                      "${operation.name.lowercase()}.")
              .build()
      throw AbortExecutionException(listOf(error))
    }
    return super.beginExecuteOperation(parameters, state)
  }
}
