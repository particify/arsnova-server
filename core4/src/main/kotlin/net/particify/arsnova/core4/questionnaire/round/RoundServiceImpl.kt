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
import net.particify.arsnova.core4.questionnaire.content.ContentRepository
import net.particify.arsnova.core4.questionnaire.event.RoundDeletedEvent
import net.particify.arsnova.core4.questionnaire.exception.ContentNotFoundException
import net.particify.arsnova.core4.questionnaire.exception.RoundsNotSupportedException
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
      roundStatsEventPublisher.publishRoundDeleted(it.id!!)
    }
    applicationEventPublisher.publishEvent(RoundDeletedEvent(contentId))
    return rounds.count()
  }

  /**
   * Replaces every round of [content] by a fresh first one, dropping their answers. The new round
   * starts closed in LIVE mode, where it opens when the content is presented, like the round of a
   * new content. The questionnaire lets go of the active round first, since it is about to be
   * deleted.
   */
  @Transactional
  fun resetRounds(content: Content): Round {
    val questionnaire = content.questionnaire!!
    val contentId = content.id!!
    if (questionnaire.activeRound?.content?.id == contentId) {
      questionnaire.activeRound = null
      questionnaireRepository.save(questionnaire)
      questionnaireEventPublisher.publishQuestionnaireUpdated(questionnaire.id!!)
    }
    content.activeRound = null
    deleteByContentId(contentId)
    val persistedRound = roundRepository.save(Round.initial(content, questionnaire.isLive()))
    content.activeRound = persistedRound
    contentRepository.save(content)
    publishRoundsChanged(content)
    return persistedRound
  }

  @Transactional
  fun startNewRound(contentId: UUID): Round {
    val content =
        contentRepository.findByIdOrNull(contentId) ?: throw ContentNotFoundException(contentId)
    val format = content.formatData!!
    if (!format.supportsRounds()) throw RoundsNotSupportedException(format.format)
    val questionnaire = content.questionnaire!!
    val live = questionnaire.isLive()
    val currentRound = content.activeRound
    currentRound?.let { stopAnswering(it) }
    // Only LIVE presents one content at a time; elsewhere other contents' rounds stay open.
    if (live) questionnaire.activeRound?.let { stopAnswering(it) }
    val round = Round(content = content, roundNo = (currentRound?.roundNo ?: 0) + 1)
    // Formats which support rounds are all answerable, so a new live round always opens.
    if (live) round.startAnswering(content.duration) else round.openWithoutCountdown()
    val persistedRound = roundRepository.save(round)
    content.activeRound = persistedRound
    val persistedContent = contentRepository.save(content)
    if (live) {
      questionnaire.activeRound = persistedRound
      questionnaire.releaseUpTo(persistedContent)
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

  @Deprecated(
      "Deprecated by base implementation", replaceWith = ReplaceWith("deleteAllInBatch(entities)"))
  override fun deleteInBatch(entities: Iterable<Round>) = roundRepository.deleteInBatch(entities)
}
