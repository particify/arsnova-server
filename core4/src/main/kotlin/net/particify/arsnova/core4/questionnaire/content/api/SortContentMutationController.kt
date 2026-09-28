/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.content.api

import net.particify.arsnova.core4.questionnaire.content.Content
import net.particify.arsnova.core4.questionnaire.content.ContentServiceImpl
import net.particify.arsnova.core4.questionnaire.content.SortContentFormatData
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

@Controller
@PreAuthorize("hasRole('USER')")
@SchemaMapping(typeName = "Mutation")
class SortContentMutationController(private val contentService: ContentServiceImpl) {
  @MutationMapping
  @PreAuthorize("hasPermission(#input.questionnaireId, 'Questionnaire', 'write')")
  fun createQuestionnaireSortContent(@Argument input: CreateSortContentInput): Content {
    return contentService.create(
        input.questionnaireId,
        input.body,
        SortContentFormatData.shuffled(input.options),
        input.duration,
        input.abstentionsAllowed,
        input.weight)
  }

  @MutationMapping
  @PreAuthorize("hasPermission(#input.id, 'Content', 'write')")
  fun updateQuestionnaireSortContent(@Argument input: UpdateSortContentInput): Content {
    return contentService.update(
        input.id,
        input.body,
        // An unchanged sequence keeps the stored order, see SortContentFormatData.mergeWith.
        SortContentFormatData.shuffled(input.options),
        input.duration,
        input.abstentionsAllowed,
        input.weight)
  }
}
