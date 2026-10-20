/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.content.api

import net.particify.arsnova.core4.questionnaire.content.BinaryContentFormatData
import net.particify.arsnova.core4.questionnaire.content.Content
import net.particify.arsnova.core4.questionnaire.content.ContentServiceImpl
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

@Controller
@PreAuthorize("hasRole('USER')")
@SchemaMapping(typeName = "Mutation")
class BinaryContentMutationController(private val contentService: ContentServiceImpl) {
  @MutationMapping
  @PreAuthorize("hasPermission(#input.questionnaireId, 'Questionnaire', 'write')")
  fun createQuestionnaireBinaryContent(@Argument input: CreateBinaryContentInput): Content {
    return contentService.create(
        input.questionnaireId,
        input.body,
        BinaryContentFormatData(input.correctAnswer),
        input.duration,
        input.abstentionsAllowed,
        input.weight)
  }

  @MutationMapping
  @PreAuthorize("hasPermission(#input.id, 'Content', 'write')")
  fun updateQuestionnaireBinaryContent(@Argument input: UpdateBinaryContentInput): Content {
    return contentService.update(
        input.id,
        input.body,
        BinaryContentFormatData(input.correctAnswer),
        input.duration,
        input.abstentionsAllowed,
        input.weight)
  }
}
