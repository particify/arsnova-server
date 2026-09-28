/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.answer.api

import java.util.UUID
import net.particify.arsnova.core4.questionnaire.answer.Answer
import net.particify.arsnova.core4.questionnaire.answer.AnswerServiceImpl
import net.particify.arsnova.core4.questionnaire.answer.ChoiceAnswerFormatData
import net.particify.arsnova.core4.questionnaire.answer.NumericAnswerFormatData
import net.particify.arsnova.core4.questionnaire.answer.PrioritizationAnswerFormatData
import net.particify.arsnova.core4.questionnaire.answer.ScaleAnswerFormatData
import net.particify.arsnova.core4.questionnaire.answer.ShortAnswerAnswerFormatData
import net.particify.arsnova.core4.questionnaire.answer.SortAnswerFormatData
import net.particify.arsnova.core4.questionnaire.answer.TextAnswerFormatData
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
  fun createQuestionnaireNumericAnswer(
      @Argument input: CreateNumericAnswerInput,
  ): Answer {
    return answerService.createAnswer(input.roundId, NumericAnswerFormatData(input.selectedNumber))
  }

  @MutationMapping
  @PreAuthorize("hasPermission(#input.roundId, 'Round', 'read')")
  fun createQuestionnairePrioritizationAnswer(
      @Argument input: CreatePrioritizationAnswerInput,
  ): Answer {
    return answerService.createAnswer(
        input.roundId, PrioritizationAnswerFormatData(input.assignedPoints))
  }

  @MutationMapping
  @PreAuthorize("hasPermission(#input.roundId, 'Round', 'read')")
  fun createQuestionnaireScaleAnswer(
      @Argument input: CreateScaleAnswerInput,
  ): Answer {
    return answerService.createAnswer(input.roundId, ScaleAnswerFormatData(input.rating))
  }

  @MutationMapping
  @PreAuthorize("hasPermission(#input.roundId, 'Round', 'read')")
  fun createQuestionnaireShortAnswerAnswer(
      @Argument input: CreateShortAnswerAnswerInput,
  ): Answer {
    return answerService.createAnswer(input.roundId, ShortAnswerAnswerFormatData(input.body))
  }

  @MutationMapping
  @PreAuthorize("hasPermission(#input.roundId, 'Round', 'read')")
  fun createQuestionnaireSortAnswer(
      @Argument input: CreateSortAnswerInput,
  ): Answer {
    return answerService.createAnswer(
        input.roundId, SortAnswerFormatData(input.selectedChoiceIndexes))
  }

  @MutationMapping
  @PreAuthorize("hasPermission(#input.roundId, 'Round', 'read')")
  fun createQuestionnaireWordcloudAnswer(
      @Argument input: CreateWordcloudAnswerInput,
  ): Answer {
    return answerService.createAnswer(input.roundId, WordcloudAnswerFormatData(input.words))
  }

  @MutationMapping
  @PreAuthorize("hasPermission(#input.roundId, 'Round', 'read')")
  fun createQuestionnaireTextAnswer(
      @Argument input: CreateTextAnswerInput,
  ): Answer {
    return answerService.createAnswer(input.roundId, TextAnswerFormatData(input.body))
  }

  @MutationMapping
  @PreAuthorize("hasPermission(#answerId, 'Answer', 'moderate')")
  fun toggleQuestionnaireTextAnswerHidden(
      @Argument answerId: UUID,
      @Argument hidden: Boolean
  ): Answer {
    return answerService.updateHidden(answerId, hidden)
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
