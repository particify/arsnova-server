/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.round

import jakarta.transaction.Transactional
import java.util.*
import kotlin.Deprecated
import kotlin.Int
import kotlin.ReplaceWith
import net.particify.arsnova.core4.questionnaire.answer.AnswerRepository
import net.particify.arsnova.core4.questionnaire.content.Content
import net.particify.arsnova.core4.questionnaire.content.ContentFormatData
import net.particify.arsnova.core4.questionnaire.content.ContentRepository
import net.particify.arsnova.core4.questionnaire.event.RoundDeletedEvent
import net.particify.arsnova.core4.questionnaire.exception.ContentNotFoundException
import net.particify.arsnova.core4.questionnaire.exception.RoundsNotSupportedException
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnairePublishingMode
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnaireRepository
import net.particify.arsnova.core4.questionnaire.questionnaire.api.QuestionnaireEventPublisher
import net.particify.arsnova.core4.questionnaire.round.api.AnswerChangedEvent
import net.particify.arsnova.core4.questionnaire.round.api.RoundStatsEventPublisher
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class RoundServiceImpl(
    private val roundRepository: RoundRepository,
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val answerRepository: AnswerRepository,
    private val contentRepository: ContentRepository,
    private val questionnaireRepository: QuestionnaireRepository,
    private val questionnaireEventPublisher: QuestionnaireEventPublisher,
    private val roundStatsEventPublisher: RoundStatsEventPublisher
) : RoundRepository by roundRepository {

  @Transactional
  fun deleteByContentId(contentId: UUID): Int {
    val rounds = roundRepository.findByContentIdOrderByRoundNo(contentId)
    rounds.forEach {
      answerRepository.deleteByRoundId(it.id!!)
      roundRepository.deleteById(it.id!!)
    }
    applicationEventPublisher.publishEvent(RoundDeletedEvent(contentId))
    return rounds.count()
  }

  @Transactional
  fun startNewRound(contentId: UUID): Round {
    val content =
        contentRepository.findByIdOrNull(contentId) ?: throw ContentNotFoundException(contentId)
    val format = content.formatData!!
    if (!format.supportsRounds()) throw RoundsNotSupportedException(format.format)
    val questionnaire = content.questionnaire!!
    val live = questionnaire.publishingMode == QuestionnairePublishingMode.LIVE
    val currentRound = content.activeRound
    currentRound?.let { stopAnswering(it) }
    // Only LIVE presents one content at a time; elsewhere other contents' rounds stay open.
    if (live) questionnaire.activeRound?.let { stopAnswering(it) }
    val round = Round(content = content, roundNo = (currentRound?.roundNo ?: 0) + 1, closed = false)
    if (live) startLiveAnswering(round, format, content.duration)
    val persistedRound = roundRepository.save(round)
    content.activeRound = persistedRound
    val persistedContent = contentRepository.save(content)
    if (live) {
      questionnaire.activeRound = persistedRound
      questionnaire.publishedUpToContent = persistedContent
      questionnaireRepository.save(questionnaire)
    }
    questionnaireEventPublisher.publishQuestionnaireUpdated(questionnaire.id!!)
    return persistedRound
  }

  /**
   * Signals every round of [content] as changed, for a change of the content that alters what its
   * rounds' stats derive from their answers. All rounds are signalled, since they may be compared.
   */
  fun publishRoundsChanged(content: Content) {
    roundRepository.findByContentIdOrderByRoundNo(content.id!!).forEach {
      roundStatsEventPublisher.publishAnswerChanged(
          AnswerChangedEvent(
              roundId = it.id!!,
              contentId = content.id!!,
              questionnaireId = content.questionnaire!!.id!!,
          ))
    }
  }

  private fun stopAnswering(round: Round) {
    round.stopAnswering()
    roundRepository.save(round)
  }

  private fun startLiveAnswering(round: Round, formatData: ContentFormatData, duration: Int?) {
    if (formatData.answerable()) {
      round.startAnswering(duration)
    } else {
      round.stopAnswering()
    }
  }

  @Deprecated(
      "Deprecated by base implementation", replaceWith = ReplaceWith("deleteAllInBatch(entities)"))
  override fun deleteInBatch(entities: Iterable<Round>) = roundRepository.deleteInBatch(entities)
}
