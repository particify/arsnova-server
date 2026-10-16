/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.content.api

import net.particify.arsnova.core4.questionnaire.answer.AnswerRepository
import net.particify.arsnova.core4.questionnaire.content.Content
import org.springframework.graphql.data.method.annotation.BatchMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

@Controller
@PreAuthorize("hasRole('USER')")
class ContentAnswersFieldController(private val answerRepository: AnswerRepository) {
  /** Same query as the server-side edit lock, across every round and hidden answers alike. */
  @BatchMapping(typeName = "Content")
  fun hasAnswers(contents: List<Content>): Map<Content, Boolean> {
    val withAnswers = answerRepository.findContentIdsWithAnswers(contents.mapNotNull { it.id })
    return contents.associateWith { it.id in withAnswers }
  }
}
