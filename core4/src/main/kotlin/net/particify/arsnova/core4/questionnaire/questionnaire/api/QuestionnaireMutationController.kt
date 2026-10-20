/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.questionnaire.api

import java.util.UUID
import net.particify.arsnova.core4.questionnaire.questionnaire.Questionnaire
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnaireServiceImpl
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

@Controller
@PreAuthorize("hasRole('USER')")
@SchemaMapping(typeName = "Mutation")
class QuestionnaireMutationController(
    private val questionnaireServiceImpl: QuestionnaireServiceImpl
) {
  @MutationMapping
  @PreAuthorize("hasPermission(#input.roomId, 'Room', 'write')")
  fun createQuestionnaire(@Argument input: CreateQuestionnaireInput): Questionnaire {
    return questionnaireServiceImpl.create(input.roomId, input.name, input.type)
  }

  @MutationMapping
  @PreAuthorize("hasPermission(#id, 'Questionnaire', 'write')")
  fun toggleQuestionnairePublished(
      @Argument id: UUID,
      @Argument published: Boolean
  ): Questionnaire {
    return questionnaireServiceImpl.updatePublished(id, published)
  }

  @MutationMapping
  @PreAuthorize("hasPermission(#id, 'Questionnaire', 'delete')")
  fun deleteQuestionnaire(@Argument id: UUID): UUID {
    questionnaireServiceImpl.delete(id)
    return id
  }

  @MutationMapping
  @PreAuthorize("hasPermission(#id, 'Questionnaire', 'write')")
  fun duplicateQuestionnaire(@Argument id: UUID, @Argument newName: String): Questionnaire {
    return questionnaireServiceImpl.duplicate(id, newName)
  }

  @MutationMapping
  @PreAuthorize("hasPermission(#input.id, 'Questionnaire', 'write')")
  fun updateQuestionnaire(@Argument input: UpdateQuestionnaireInput): Questionnaire {
    return questionnaireServiceImpl.update(
        input.id,
        input.name,
        input.published,
        input.publishingMode,
        input.statisticsPublished,
        input.correctOptionsPublished,
        input.leaderboardEnabled)
  }

  @MutationMapping
  @PreAuthorize("hasPermission(#questionnaireId, 'Questionnaire', 'write')")
  fun updateQuestionnaireContentOrder(
      @Argument questionnaireId: UUID,
      @Argument contentIds: List<UUID>
  ): Questionnaire {
    return questionnaireServiceImpl.updateContentOrder(questionnaireId, contentIds)
  }

  @MutationMapping
  @PreAuthorize("hasPermission(#id, 'Questionnaire', 'write')")
  fun resetQuestionnaireResults(@Argument id: UUID): Boolean {
    return questionnaireServiceImpl.resetAnswerResultsByQuestionnaireId(id)
  }
}
