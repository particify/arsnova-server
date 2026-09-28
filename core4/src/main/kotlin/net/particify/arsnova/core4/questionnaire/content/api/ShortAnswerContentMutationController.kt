/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.content.api

import java.util.UUID
import net.particify.arsnova.core4.questionnaire.content.Content
import net.particify.arsnova.core4.questionnaire.content.ContentServiceImpl
import net.particify.arsnova.core4.questionnaire.content.ShortAnswerContentFormatData
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

@Controller
@PreAuthorize("hasRole('USER')")
@SchemaMapping(typeName = "Mutation")
class ShortAnswerContentMutationController(private val contentService: ContentServiceImpl) {
  @MutationMapping
  @PreAuthorize("hasPermission(#input.questionnaireId, 'Questionnaire', 'write')")
  fun createQuestionnaireShortAnswerContent(
      @Argument input: CreateShortAnswerContentInput
  ): Content {
    return contentService.create(
        input.questionnaireId,
        input.body,
        ShortAnswerContentFormatData(input.correctTerms),
        input.duration,
        input.abstentionsAllowed,
        input.weight)
  }

  @MutationMapping
  @PreAuthorize("hasPermission(#input.id, 'Content', 'write')")
  fun updateQuestionnaireShortAnswerContent(
      @Argument input: UpdateShortAnswerContentInput
  ): Content {
    return contentService.update(
        input.id,
        input.body,
        ShortAnswerContentFormatData(input.correctTerms),
        input.duration,
        input.abstentionsAllowed,
        input.weight)
  }

  @MutationMapping
  @PreAuthorize("hasPermission(#contentId, 'Content', 'write')")
  fun banQuestionnaireShortAnswerContentKeyword(
      @Argument contentId: UUID,
      @Argument keyword: String
  ): Content {
    return contentService.banShortAnswerKeyword(contentId, keyword)
  }

  @MutationMapping
  @PreAuthorize("hasPermission(#contentId, 'Content', 'write')")
  fun resetBannedQuestionnaireShortAnswerContentKeywords(@Argument contentId: UUID): Content {
    return contentService.resetBannedShortAnswerKeywords(contentId)
  }
}
