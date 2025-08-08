/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.qna.exception

import graphql.GraphQLError
import graphql.GraphqlErrorBuilder
import graphql.schema.DataFetchingEnvironment
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter
import org.springframework.graphql.execution.ErrorType
import org.springframework.stereotype.Component

@Component
class QnaExceptionResolverAdapter : DataFetcherExceptionResolverAdapter() {
  override fun resolveToSingleError(ex: Throwable, env: DataFetchingEnvironment): GraphQLError? {
    val builder = GraphqlErrorBuilder.newError(env)
    val error =
        when (ex) {
          is QnaNotFoundException ->
              builder
                  .errorType(ErrorType.NOT_FOUND)
                  .message(if (ex.id != null) "${ex.message}: ${ex.id}" else ex.message)
          is PostNotFoundException ->
              builder
                  .errorType(ErrorType.NOT_FOUND)
                  .message(if (ex.id != null) "${ex.message}: ${ex.id}" else ex.message)
          else -> return super.resolveToSingleError(ex, env)
        }
    return error.build()
  }
}
