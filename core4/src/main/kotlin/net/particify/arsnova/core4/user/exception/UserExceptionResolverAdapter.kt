/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user.exception

import graphql.GraphQLError
import graphql.GraphqlErrorBuilder
import graphql.schema.DataFetchingEnvironment
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter
import org.springframework.graphql.execution.ErrorType
import org.springframework.stereotype.Component

/** Lets a client tell a rejected password apart from other bad requests without its message. */
const val WEAK_PASSWORD_ERROR_CODE = "WEAK_PASSWORD"

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
          is InvalidVerificationCodeException,
          is MailAddressAlreadyInUseException,
          is MailAddressNotAllowedException ->
              builder.errorType(ErrorType.BAD_REQUEST).message(ex.message)
          is WeakPasswordException ->
              builder
                  .errorType(ErrorType.BAD_REQUEST)
                  .message(ex.message)
                  .extensions(mapOf("code" to WEAK_PASSWORD_ERROR_CODE))
          is AccountCreationNotAllowedException ->
              builder.errorType(ErrorType.FORBIDDEN).message(ex.message)
          else -> return super.resolveToSingleError(ex, env)
        }
    return error.build()
  }
}
