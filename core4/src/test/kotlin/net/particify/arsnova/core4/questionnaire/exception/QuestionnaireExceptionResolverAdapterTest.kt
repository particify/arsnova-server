/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.exception

import graphql.GraphQLError
import graphql.Scalars
import graphql.execution.ExecutionStepInfo
import graphql.execution.MergedField
import graphql.execution.ResultPath
import graphql.language.Field
import graphql.schema.DataFetchingEnvironmentImpl
import java.util.UUID
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.graphql.execution.ErrorType
import reactor.test.StepVerifier

private val ID = UUID.randomUUID()

class QuestionnaireExceptionResolverAdapterTest {
  private val resolver = QuestionnaireExceptionResolverAdapter()
  private val environment =
      DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
          .mergedField(MergedField.newMergedField(Field("field")).build())
          .executionStepInfo(
              ExecutionStepInfo.newExecutionStepInfo()
                  .type(Scalars.GraphQLString)
                  .path(ResultPath.rootPath())
                  .build())
          .build()

  private fun resolve(ex: Throwable): GraphQLError? =
      resolver.resolveException(ex, environment).block()?.single()

  @Test
  fun shouldReportMissingEntitiesAsNotFoundWithTheirId() {
    listOf(
            AnswerNotFoundException(ID),
            ContentNotFoundException(ID),
            QuestionnaireNotFoundException(ID),
            RoundNotFoundException(ID))
        .forEach {
          val error = resolve(it)
          Assertions.assertEquals(ErrorType.NOT_FOUND, error?.errorType, it.message)
          Assertions.assertEquals("${it.message}: $ID", error?.message)
        }
  }

  @Test
  fun shouldOmitTheIdOfMissingEntityWhenUnknown() {
    Assertions.assertEquals("Content not found", resolve(ContentNotFoundException())?.message)
  }

  @Test
  fun shouldReportRejectedRequestsAsBadRequest() {
    listOf(
            ContentFormatDataInvalidException("invalid"),
            QuestionnaireWithNameAlreadyExistsException(),
            RoundClosedException(ID),
            RoundExpiredException(ID),
            RoundsNotSupportedException("FORMAT"),
            RoomlessQuestionnaireException(ID))
        .forEach {
          val error = resolve(it)
          Assertions.assertEquals(ErrorType.BAD_REQUEST, error?.errorType, it.message)
          Assertions.assertEquals(it.message, error?.message)
        }
  }

  @Test
  fun shouldLeaveOtherExceptionsToOtherResolvers() {
    StepVerifier.create(resolver.resolveException(IllegalStateException(), environment))
        .verifyComplete()
  }
}
