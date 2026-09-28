/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.questionnaire

import jakarta.transaction.Transactional
import java.util.UUID
import net.particify.arsnova.core4.questionnaire.content.ContentRepository
import net.particify.arsnova.core4.questionnaire.exception.ContentNotFoundException
import net.particify.arsnova.core4.questionnaire.exception.QuestionnaireNotFoundException
import net.particify.arsnova.core4.questionnaire.questionnaire.api.QuestionnaireEventPublisher
import net.particify.arsnova.core4.questionnaire.round.Round
import net.particify.arsnova.core4.questionnaire.round.RoundRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class QuestionnairePublishingServiceImpl(
    private val contentRepository: ContentRepository,
    private val questionnaireRepository: QuestionnaireRepository,
    private val roundRepository: RoundRepository,
    private val questionnaireEventPublisher: QuestionnaireEventPublisher
) {

  @Transactional
  fun startLiveContent(contentId: UUID): Questionnaire {
    val content =
        contentRepository.findByIdOrNull(contentId) ?: throw ContentNotFoundException(contentId)
    val questionnaire = content.questionnaire!!
    questionnaire.activeRound?.let {
      it.stopAnswering()
      roundRepository.save(it)
    }
    val round = content.activeRound ?: Round(content = content, roundNo = 1)
    if (content.formatData!!.answerable()) {
      round.openAnswering(content.duration)
    } else {
      round.stopAnswering()
    }
    val persistedRound = roundRepository.save(round)
    content.activeRound = persistedRound
    val persistedContent = contentRepository.save(content)
    questionnaire.activeRound = persistedRound
    questionnaire.publishedUpToContent = persistedContent
    val persistedQuestionnaire = questionnaireRepository.save(questionnaire)
    questionnaireEventPublisher.publishQuestionnaireUpdated(questionnaire.id!!)
    return persistedQuestionnaire
  }

  @Transactional
  fun stopLiveContent(questionnaireId: UUID): Questionnaire {
    val questionnaire =
        questionnaireRepository.findByIdOrNull(questionnaireId)
            ?: throw QuestionnaireNotFoundException(questionnaireId)
    val round =
        questionnaire.activeRound
            ?: throw IllegalStateException("No active round found for $questionnaireId")
    round.stopAnswering()
    val persistedRound = roundRepository.save(round)
    questionnaire.activeRound = persistedRound
    val persistedQuestionnaire = questionnaireRepository.save(questionnaire)
    questionnaireEventPublisher.publishQuestionnaireUpdated(questionnaireId)
    return persistedQuestionnaire
  }

  @Transactional
  fun publishUpToContent(questionnaireId: UUID, contentId: UUID): Questionnaire {
    val questionnaire =
        questionnaireRepository.findByIdOrNull(questionnaireId)
            ?: throw QuestionnaireNotFoundException(questionnaireId)
    val content =
        contentRepository.findByIdOrNull(contentId)?.takeIf {
          it.questionnaire?.id == questionnaireId
        } ?: throw ContentNotFoundException(contentId)
    questionnaire.publishedUpToContent = content
    val persistedQuestionnaire = questionnaireRepository.save(questionnaire)
    questionnaireEventPublisher.publishQuestionnaireUpdated(questionnaireId)
    return persistedQuestionnaire
  }
}
