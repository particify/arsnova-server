/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.answer.api

import java.util.UUID
import net.particify.arsnova.core4.questionnaire.answer.Answer
import net.particify.arsnova.core4.questionnaire.answer.AnswerServiceImpl
import net.particify.arsnova.core4.questionnaire.answer.ChoiceAnswerFormatData
import net.particify.arsnova.core4.questionnaire.answer.WordcloudAnswerFormatData
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

@Controller
@PreAuthorize("hasRole('USER')")
@SchemaMapping(typeName = "Mutation")
class AnswerMutationController(private val answerService: AnswerServiceImpl) {
  @MutationMapping
  @PreAuthorize("hasPermission(#input.roundId, 'Round', 'read')")
  fun createQuestionnaireChoiceAnswer(
      @Argument input: CreateChoiceAnswerInput,
  ): Answer {
    return answerService.createAnswer(
        input.roundId, ChoiceAnswerFormatData(input.selectedChoiceIndexes))
  }

  @MutationMapping
  @PreAuthorize("hasPermission(#input.roundId, 'Round', 'read')")
  fun createQuestionnaireWordcloudAnswer(
      @Argument input: CreateWordcloudAnswerInput,
  ): Answer {
    return answerService.createAnswer(input.roundId, WordcloudAnswerFormatData(input.words))
  }

  @MutationMapping
  @PreAuthorize("hasPermission(#roundId, 'Round', 'read')")
  fun createQuestionnaireAbstention(
      @Argument roundId: UUID,
  ): Answer {
    return answerService.createAbstention(roundId)
  }

  @MutationMapping
  @PreAuthorize("hasPermission(#contentId, 'Content', 'write')")
  fun deleteQuestionnaireAnswersByContentId(@Argument contentId: UUID): Boolean {
    return answerService.deleteByContentId(contentId)
  }
}
