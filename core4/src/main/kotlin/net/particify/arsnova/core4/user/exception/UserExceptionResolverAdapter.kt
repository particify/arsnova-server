/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user.exception

import graphql.GraphQLError
import graphql.GraphqlErrorBuilder
import graphql.schema.DataFetchingEnvironment
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter
import org.springframework.graphql.execution.ErrorType
import org.springframework.stereotype.Component

@Component
class UserExceptionResolverAdapter : DataFetcherExceptionResolverAdapter() {
  override fun resolveToSingleError(ex: Throwable, env: DataFetchingEnvironment): GraphQLError? {
    val builder = GraphqlErrorBuilder.newError(env)
    val error =
        when (ex) {
          is UserNotFoundException ->
              builder
                  .errorType(ErrorType.NOT_FOUND)
                  .message(if (ex.id != null) "${ex.message}: ${ex.id}" else ex.message)
          is InvalidUserStateException ->
              builder.errorType(ErrorType.BAD_REQUEST).message("${ex.message}: ${ex.id}")
          is InvalidVerificationCodeException ->
              builder.errorType(ErrorType.BAD_REQUEST).message(ex.message)
          else -> return super.resolveToSingleError(ex, env)
        }
    return error.build()
  }
}
