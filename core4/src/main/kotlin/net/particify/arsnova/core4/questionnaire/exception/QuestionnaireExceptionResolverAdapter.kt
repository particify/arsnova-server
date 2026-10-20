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
    val notFound = notFoundMessage(ex)
    val error =
        when {
          notFound != null -> builder.errorType(ErrorType.NOT_FOUND).message(notFound)
          isBadRequest(ex) -> builder.errorType(ErrorType.BAD_REQUEST).message(ex.message)
          else -> return super.resolveToSingleError(ex, env)
        }
    return error.build()
  }

  private fun isBadRequest(ex: Throwable): Boolean =
      when (ex) {
        is AbstentionsNotAllowedException,
        is AnswerAlreadyExistsException,
        is ContentFormatDataInvalidException,
        is QuestionnaireWithNameAlreadyExistsException,
        is RoundClosedException,
        is RoundExpiredException,
        is RoundsNotSupportedException -> true
        else -> false
      }
}

/** The message reporting a missing entity of the questionnaire module, with its id if known. */
internal fun notFoundMessage(ex: Throwable): String? {
  val id =
      when (ex) {
        is AnswerNotFoundException -> ex.id
        is ContentNotFoundException -> ex.id
        is QuestionnaireNotFoundException -> ex.id
        is RoundNotFoundException -> ex.id
        else -> return null
      }
  return if (id != null) "${ex.message}: $id" else ex.message
}
