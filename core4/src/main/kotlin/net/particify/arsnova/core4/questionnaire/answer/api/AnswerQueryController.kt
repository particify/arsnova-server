/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.answer.api

import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import net.particify.arsnova.core4.questionnaire.answer.Answer
import net.particify.arsnova.core4.questionnaire.answer.AnswerResultVisibility
import net.particify.arsnova.core4.questionnaire.answer.AnswerServiceImpl
import net.particify.arsnova.core4.questionnaire.answer.ContentAnswerResult
import net.particify.arsnova.core4.questionnaire.answer.QuestionnaireAnswerResult
import net.particify.arsnova.core4.user.User
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.graphql.data.query.ScrollSubrange
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller

@Controller
@PreAuthorize("hasRole('USER')")
@SchemaMapping(typeName = "Query")
class AnswerQueryController(
    private val answerServiceImpl: AnswerServiceImpl,
    private val answerResultVisibility: AnswerResultVisibility
) {

  @QueryMapping
  @PreAuthorize("hasPermission(#roundId, 'Round', 'read')")
  fun questionnaireAnswerByRoundId(
      @Argument roundId: UUID,
      @AuthenticationPrincipal user: User
  ): Answer? {
    return answerServiceImpl.findByRoundIdAndAuditMetadataCreatedBy(
        roundId,
        user.id!!,
    )
  }

  @QueryMapping
  @PreAuthorize("hasPermission(#questionnaireId, 'Questionnaire', 'read')")
  fun questionnaireContentAnswerResultsByQuestionnaireId(
      @Argument questionnaireId: UUID,
      @AuthenticationPrincipal user: User,
      subrange: ScrollSubrange,
  ): List<ContentAnswerResult> {
    return answerResultVisibility.contentResults(
        questionnaireId,
        answerServiceImpl.findQuestionnaireContentAnswerResults(
            questionnaireId, user.id!!, subrange))
  }

  @QueryMapping
  @PreAuthorize("hasPermission(#questionnaireId, 'Questionnaire', 'read')")
  fun questionnaireAnswerResultsByQuestionnaireId(
      @Argument questionnaireId: UUID,
      @AuthenticationPrincipal user: User,
      subrange: ScrollSubrange,
  ): QuestionnaireAnswerResult {
    return answerResultVisibility.questionnaireResult(
        answerServiceImpl.findQuestionnaireAnswerResults(questionnaireId, user.id!!, subrange))
  }

  @SchemaMapping(typeName = "Answer")
  fun createdAt(answer: Answer): OffsetDateTime? {
    return answer.auditMetadata.createdAt?.atOffset(ZoneOffset.UTC)
  }
}
