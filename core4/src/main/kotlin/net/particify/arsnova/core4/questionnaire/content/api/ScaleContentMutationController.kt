/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.content.api

import net.particify.arsnova.core4.questionnaire.content.Content
import net.particify.arsnova.core4.questionnaire.content.ContentServiceImpl
import net.particify.arsnova.core4.questionnaire.content.ScaleContentFormatData
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

@Controller
@PreAuthorize("hasRole('USER')")
@SchemaMapping(typeName = "Mutation")
class ScaleContentMutationController(private val contentService: ContentServiceImpl) {
  @MutationMapping
  @PreAuthorize("hasPermission(#input.questionnaireId, 'Questionnaire', 'write')")
  fun createQuestionnaireScaleContent(@Argument input: CreateScaleContentInput): Content {
    return contentService.create(
        input.questionnaireId,
        input.body,
        ScaleContentFormatData(input.template, input.neutralOption),
        input.duration,
        input.abstentionsAllowed,
        null)
  }

  @MutationMapping
  @PreAuthorize("hasPermission(#input.id, 'Content', 'write')")
  fun updateQuestionnaireScaleContent(@Argument input: UpdateScaleContentInput): Content {
    return contentService.update(
        input.id,
        input.body,
        ScaleContentFormatData(input.template, input.neutralOption),
        input.duration,
        input.abstentionsAllowed,
        null)
  }
}
