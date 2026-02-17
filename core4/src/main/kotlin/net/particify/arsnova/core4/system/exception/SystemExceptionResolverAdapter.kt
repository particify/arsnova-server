/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.exception

import graphql.GraphQLError
import graphql.GraphqlErrorBuilder
import graphql.schema.DataFetchingEnvironment
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter
import org.springframework.graphql.execution.ErrorType
import org.springframework.stereotype.Component

@Component
class SystemExceptionResolverAdapter : DataFetcherExceptionResolverAdapter() {
  override fun resolveToSingleError(ex: Throwable, env: DataFetchingEnvironment): GraphQLError? {
    val builder = GraphqlErrorBuilder.newError(env)
    val error =
        when (ex) {
          is MailAddressTemporarilyBlockedException ->
              builder.errorType(ErrorType.FORBIDDEN).message(ex.message)
          else -> return super.resolveToSingleError(ex, env)
        }
    return error.build()
  }
}
