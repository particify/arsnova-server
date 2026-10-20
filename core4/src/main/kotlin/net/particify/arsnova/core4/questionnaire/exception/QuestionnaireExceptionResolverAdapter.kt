/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.exception

import graphql.GraphQLError
import graphql.GraphqlErrorBuilder
import graphql.schema.DataFetchingEnvironment
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter
import org.springframework.graphql.execution.ErrorType
import org.springframework.stereotype.Component

@Component
class QuestionnaireExceptionResolverAdapter : DataFetcherExceptionResolverAdapter() {
  override fun resolveToSingleError(ex: Throwable, env: DataFetchingEnvironment): GraphQLError? {
    val builder = GraphqlErrorBuilder.newError(env)
    val error =
        when (ex) {
          is QuestionnaireNotFoundException ->
              builder
                  .errorType(ErrorType.NOT_FOUND)
                  .message(if (ex.id != null) "${ex.message}: ${ex.id}" else ex.message)
          is ContentNotFoundException ->
              builder
                  .errorType(ErrorType.NOT_FOUND)
                  .message(if (ex.id != null) "${ex.message}: ${ex.id}" else ex.message)
          is RoundNotFoundException ->
              builder
                  .errorType(ErrorType.NOT_FOUND)
                  .message(if (ex.id != null) "${ex.message}: ${ex.id}" else ex.message)
          is AnswerNotFoundException ->
              builder
                  .errorType(ErrorType.NOT_FOUND)
                  .message(if (ex.id != null) "${ex.message}: ${ex.id}" else ex.message)
          is AbstentionsNotAllowedException,
          is AnswerAlreadyExistsException,
          is ContentFormatDataInvalidException,
          is QuestionnaireWithNameAlreadyExistsException,
          is RoundClosedException,
          is RoundExpiredException,
          is RoundsNotSupportedException ->
              builder.errorType(ErrorType.BAD_REQUEST).message(ex.message)
          else -> return super.resolveToSingleError(ex, env)
        }
    return error.build()
  }
}
