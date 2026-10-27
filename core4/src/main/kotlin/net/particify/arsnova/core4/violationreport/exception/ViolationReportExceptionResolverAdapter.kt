/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.violationreport.exception

import graphql.GraphQLError
import graphql.GraphqlErrorBuilder
import graphql.schema.DataFetchingEnvironment
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter
import org.springframework.graphql.execution.ErrorType
import org.springframework.stereotype.Component

@Component
class ViolationReportExceptionResolverAdapter : DataFetcherExceptionResolverAdapter() {
  override fun resolveToSingleError(ex: Throwable, env: DataFetchingEnvironment): GraphQLError? {
    val builder = GraphqlErrorBuilder.newError(env)
    val error =
        when (ex) {
          is ViolationReportAlreadyDecidedException ->
              builder.errorType(ErrorType.BAD_REQUEST).message("${ex.message}: ${ex.id}")
          is ViolationReportNotFoundException ->
              builder.errorType(ErrorType.NOT_FOUND).message("${ex.message}: ${ex.id}")
          else -> return super.resolveToSingleError(ex, env)
        }
    return error.build()
  }
}
