/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.content.api

import java.util.UUID
import net.particify.arsnova.core4.questionnaire.content.ChoiceContentFormatData
import net.particify.arsnova.core4.questionnaire.content.Content
import net.particify.arsnova.core4.questionnaire.content.ContentServiceImpl
import net.particify.arsnova.core4.questionnaire.content.WordcloudContentFormatData
import net.particify.arsnova.core4.questionnaire.exception.ContentNotFoundException
import net.particify.arsnova.core4.questionnaire.exception.QuestionnaireNotFoundException
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnaireServiceImpl
import org.springframework.data.repository.findByIdOrNull
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

@Controller
@PreAuthorize("hasRole('USER')")
@SchemaMapping(typeName = "Mutation")
class ContentMutationController(
    private val contentService: ContentServiceImpl,
    private val questionnaireService: QuestionnaireServiceImpl
) {
  @MutationMapping
  @PreAuthorize("hasPermission(#input.questionnaireId, 'Questionnaire', 'write')")
  fun createQuestionnaireChoiceContent(@Argument input: CreateChoiceContentInput): Content {
    return contentService.create(
        input.questionnaireId,
        input.body,
        ChoiceContentFormatData(input.options, input.correctOptionIndexes, input.multiple),
        input.duration,
        input.abstentionsAllowed,
        input.weight)
  }

  @MutationMapping
  @PreAuthorize("hasPermission(#input.id, 'Content', 'write')")
  fun updateQuestionnaireChoiceContent(@Argument input: UpdateChoiceContentInput): Content {
    return contentService.update(
        input.id,
        input.body,
        ChoiceContentFormatData(input.options, input.correctOptionIndexes, input.multiple),
        input.duration,
        input.abstentionsAllowed,
        input.weight)
  }

  @MutationMapping
  @PreAuthorize("hasPermission(#input.questionnaireId, 'Questionnaire', 'write')")
  fun createQuestionnaireWordcloudContent(@Argument input: CreateWordcloudContentInput): Content {
    return contentService.create(
        input.questionnaireId,
        input.body,
        WordcloudContentFormatData(input.maxAnswers),
        input.duration,
        input.abstentionsAllowed,
        null)
  }

  @MutationMapping
  @PreAuthorize("hasPermission(#input.id, 'Content', 'write')")
  fun updateQuestionnaireWordcloudContent(@Argument input: UpdateWordcloudContentInput): Content {
    return contentService.update(
        input.id,
        input.body,
        WordcloudContentFormatData(input.maxAnswers),
        input.duration,
        input.abstentionsAllowed,
        null)
  }

  @MutationMapping
  @PreAuthorize("hasPermission(#contentId, 'Content', 'write')")
  fun banQuestionnaireWordcloudContentKeyword(
      @Argument contentId: UUID,
      @Argument keyword: String
  ): Content {
    return contentService.banWordcloudKeyword(contentId, keyword)
  }

  @MutationMapping
  @PreAuthorize("hasPermission(#contentId, 'Content', 'write')")
  fun resetBannedQuestionnaireWordcloudContentKeywords(@Argument contentId: UUID): Content {
    return contentService.resetBannedWordcloudKeywords(contentId)
  }

  @MutationMapping
  @PreAuthorize("hasPermission(#id, 'Content', 'write')")
  fun duplicateQuestionnaireContent(@Argument id: UUID): Content {
    val content = contentService.findByIdOrNull(id) ?: throw ContentNotFoundException(id)
    return contentService.duplicate(content)
  }

  @MutationMapping
  @PreAuthorize(
      "hasPermission(#contentId, 'Content', 'read') and " +
          "hasPermission(#questionnaireId, 'Questionnaire', 'write')")
  fun copyQuestionnaireContentToQuestionnaire(
      @Argument contentId: UUID,
      @Argument questionnaireId: UUID
  ): Content {
    val content =
        contentService.findByIdOrNull(contentId) ?: throw ContentNotFoundException(contentId)
    val questionnaire =
        questionnaireService.findByIdOrNull(questionnaireId)
            ?: throw QuestionnaireNotFoundException(questionnaireId)
    return contentService.duplicate(content, questionnaire)
  }

  @MutationMapping
  @PreAuthorize(
      "hasPermission(#contentId, 'Content', 'write') and " +
          "hasPermission(#questionnaireId, 'Questionnaire', 'write')")
  fun moveQuestionnaireContentToQuestionnaire(
      @Argument contentId: UUID,
      @Argument questionnaireId: UUID
  ): Content {
    return contentService.moveContentToQuestionnaire(contentId, questionnaireId)
  }

  @MutationMapping
  @PreAuthorize("hasPermission(#id, 'Content', 'delete')")
  fun deleteQuestionnaireContent(@Argument id: UUID): UUID {
    return contentService.delete(id)
  }
}
