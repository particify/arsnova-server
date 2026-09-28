/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.content.api

import net.particify.arsnova.core4.questionnaire.content.Content
import net.particify.arsnova.core4.questionnaire.content.ContentServiceImpl
import net.particify.arsnova.core4.questionnaire.content.NumericContentFormatData
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

@Controller
@PreAuthorize("hasRole('USER')")
@SchemaMapping(typeName = "Mutation")
class NumericContentMutationController(private val contentService: ContentServiceImpl) {
  @MutationMapping
  @PreAuthorize("hasPermission(#input.questionnaireId, 'Questionnaire', 'write')")
  fun createQuestionnaireNumericContent(@Argument input: CreateNumericContentInput): Content {
    return contentService.create(
        input.questionnaireId,
        input.body,
        formatData(input.minNumber, input.maxNumber, input.correctNumber, input.tolerance),
        input.duration,
        input.abstentionsAllowed,
        input.weight)
  }

  @MutationMapping
  @PreAuthorize("hasPermission(#input.id, 'Content', 'write')")
  fun updateQuestionnaireNumericContent(@Argument input: UpdateNumericContentInput): Content {
    return contentService.update(
        input.id,
        input.body,
        formatData(input.minNumber, input.maxNumber, input.correctNumber, input.tolerance),
        input.duration,
        input.abstentionsAllowed,
        input.weight)
  }

  private fun formatData(
      minNumber: Double,
      maxNumber: Double,
      correctNumber: Double?,
      tolerance: Double?
  ) = NumericContentFormatData(minNumber, maxNumber, correctNumber, tolerance ?: 0.0)
}
