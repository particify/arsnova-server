/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.content.api

import java.util.UUID
import net.particify.arsnova.core4.common.TextRenderingService
import net.particify.arsnova.core4.questionnaire.answer.AnswerRepository
import net.particify.arsnova.core4.questionnaire.answer.AnswerResultState
import net.particify.arsnova.core4.questionnaire.content.AnswerOption
import net.particify.arsnova.core4.questionnaire.content.ChoiceContentFormatData
import net.particify.arsnova.core4.questionnaire.content.Content
import net.particify.arsnova.core4.questionnaire.content.ContentServiceImpl
import net.particify.arsnova.core4.questionnaire.exception.ContentNotFoundException
import net.particify.arsnova.core4.questionnaire.round.Round
import net.particify.arsnova.core4.questionnaire.round.RoundServiceImpl
import org.springframework.data.domain.ScrollPosition
import org.springframework.data.domain.Window
import org.springframework.data.repository.findByIdOrNull
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.graphql.data.query.ScrollSubrange
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

@Controller
@PreAuthorize("hasRole('USER')")
@SchemaMapping(typeName = "Query")
class ContentQueryController(
    private val contentServiceImpl: ContentServiceImpl,
    private val textRenderingService: TextRenderingService,
    private val roundService: RoundServiceImpl,
    private val answerRepository: AnswerRepository
) {

  @QueryMapping
  @PreAuthorize("hasPermission(#questionnaireId, 'Questionnaire', 'write')")
  fun questionnaireContentsByQuestionnaireId(
      @Argument questionnaireId: UUID,
      subrange: ScrollSubrange
  ): Window<Content> {
    return contentServiceImpl.findByQuestionnaireIdOrderByPosition(
        questionnaireId, subrange.position().orElse(ScrollPosition.offset()))
  }

  @QueryMapping
  @PreAuthorize("hasPermission(#questionnaireId, 'Questionnaire', 'read')")
  fun questionnairePublishedContentsByQuestionnaireId(
      @Argument questionnaireId: UUID,
      subrange: ScrollSubrange
  ): Window<Content> {
    return contentServiceImpl.findByQuestionnaireIdOrderByPositionForCurrentUser(
        questionnaireId,
        subrange.position().orElse(ScrollPosition.offset()),
    )
  }

  @SchemaMapping(typeName = "Content")
  fun bodyRendered(content: Content): String? {
    return textRenderingService.renderText(content.body)
  }

  @SchemaMapping(typeName = "Content")
  fun activeRound(content: Content): Round? {
    return roundService.findByContentIdOrderByRoundNo(content.id!!).lastOrNull()
  }

  @SchemaMapping(typeName = "ChoiceContentFormatData")
  fun options(formatData: ChoiceContentFormatData): List<AnswerOption> =
      answerOptions(formatData.options)

  private fun answerOptions(labels: List<String>): List<AnswerOption> =
      labels.map { AnswerOption(label = it, labelRendered = textRenderingService.renderText(it)) }

  @SchemaMapping(typeName = "Content")
  fun answerCount(content: Content): Int {
    val roundId = activeRound(content)?.id
    return if (roundId != null) {
      answerRepository.countByRoundIdAndHiddenFalse(roundId)
    } else {
      0
    }
  }

  @SchemaMapping(typeName = "Content")
  fun abstentionCount(content: Content): Int {
    val roundId = activeRound(content)?.id
    return if (roundId != null) {
      answerRepository.countByRoundIdAndResult(roundId, AnswerResultState.ABSTAINED)
    } else {
      0
    }
  }

  @SchemaMapping(typeName = "Content")
  fun correctAnswerCount(content: Content): Int {
    val roundId = activeRound(content)?.id
    return if (roundId != null) {
      answerRepository.countByRoundIdAndResult(roundId, AnswerResultState.CORRECT)
    } else {
      0
    }
  }

  @SchemaMapping(typeName = "Content")
  fun hasCorrectAnswer(content: Content): Boolean {
    return content.formatData!!.hasCorrectAnswer()
  }

  @SchemaMapping
  @PreAuthorize("hasPermission(#id, 'Content', 'read')")
  fun questionnaireContentById(@Argument id: UUID): Content {
    return contentServiceImpl.findByIdOrNull(id) ?: throw ContentNotFoundException(id)
  }
}
