/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.content.api

import java.util.UUID
import net.particify.arsnova.core4.questionnaire.content.Content
import net.particify.arsnova.core4.questionnaire.content.ContentServiceImpl
import net.particify.arsnova.core4.questionnaire.content.TextContentFormatData
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

@Controller
@PreAuthorize("hasRole('USER')")
@SchemaMapping(typeName = "Mutation")
class TextContentMutationController(private val contentService: ContentServiceImpl) {
  @MutationMapping
  @PreAuthorize("hasPermission(#input.questionnaireId, 'Questionnaire', 'write')")
  fun createQuestionnaireTextContent(@Argument input: CreateTextContentInput): Content {
    return contentService.create(
        input.questionnaireId,
        input.body,
        TextContentFormatData(false),
        input.duration,
        input.abstentionsAllowed,
        null)
  }

  @MutationMapping
  @PreAuthorize("hasPermission(#input.id, 'Content', 'write')")
  fun updateQuestionnaireTextContent(@Argument input: UpdateTextContentInput): Content {
    return contentService.update(
        input.id,
        input.body,
        TextContentFormatData(false),
        input.duration,
        input.abstentionsAllowed,
        null)
  }

  @MutationMapping
  @PreAuthorize("hasPermission(#contentId, 'Content', 'write')")
  fun toggleQuestionnaireTextContentAnswersPublished(
      @Argument contentId: UUID,
      @Argument published: Boolean
  ): Content {
    return contentService.updateTextAnswersPublished(contentId, published)
  }
}
