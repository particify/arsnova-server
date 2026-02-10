/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.common.exception

import graphql.GraphQLError
import graphql.GraphqlErrorBuilder
import graphql.schema.DataFetchingEnvironment
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter
import org.springframework.graphql.execution.ErrorType
import org.springframework.stereotype.Component
import org.springframework.validation.BindException

@Component
class CommonExceptionResolverAdapter : DataFetcherExceptionResolverAdapter() {
  override fun resolveToSingleError(ex: Throwable, env: DataFetchingEnvironment): GraphQLError? {
    val builder = GraphqlErrorBuilder.newError(env)
    val error =
        when (ex) {
          is BindException ->
              builder
                  .errorType(ErrorType.BAD_REQUEST)
                  .message(ex.bindingResult.allErrors.toString())
          is AccessDeniedException -> builder.errorType(ErrorType.FORBIDDEN).message(ex.message)
          is InvalidInputException -> builder.errorType(ErrorType.BAD_REQUEST).message(ex.message)
          else -> return super.resolveToSingleError(ex, env)
        }
    return error.build()
  }
}
