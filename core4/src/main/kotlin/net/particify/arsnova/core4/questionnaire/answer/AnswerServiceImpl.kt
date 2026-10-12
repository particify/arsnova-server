/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.answer

import jakarta.transaction.Transactional
import java.time.Instant
import java.util.UUID
import net.particify.arsnova.core4.questionnaire.content.Content
import net.particify.arsnova.core4.questionnaire.content.ContentServiceImpl
import net.particify.arsnova.core4.questionnaire.exception.AnswerAlreadyExistsException
import net.particify.arsnova.core4.questionnaire.exception.QuestionnaireNotFoundException
import net.particify.arsnova.core4.questionnaire.exception.RoundClosedException
import net.particify.arsnova.core4.questionnaire.exception.RoundExpiredException
import net.particify.arsnova.core4.questionnaire.exception.RoundNotFoundException
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnaireRepository
import net.particify.arsnova.core4.questionnaire.round.Round
import net.particify.arsnova.core4.questionnaire.round.RoundRepository
import net.particify.arsnova.core4.questionnaire.round.api.AnswerChangedEvent
import net.particify.arsnova.core4.questionnaire.round.api.RoundStatsEventPublisher
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.AuditorAware
import org.springframework.data.domain.ScrollPosition
import org.springframework.data.repository.findByIdOrNull
import org.springframework.graphql.data.query.ScrollSubrange
import org.springframework.stereotype.Service

@Service
class AnswerServiceImpl(
    private val answerRepository: AnswerRepository,
    private val roundRepository: RoundRepository,
    private val contentServiceImpl: ContentServiceImpl,
    private val roundStatsEventPublisher: RoundStatsEventPublisher,
    private val questionnaireRepository: QuestionnaireRepository,
    private val auditorAware: AuditorAware<UUID>,
) : AnswerRepository by answerRepository {

  private fun findAnswerableRound(roundId: UUID): Round {
    val round = roundRepository.findByIdOrNull(roundId) ?: throw RoundNotFoundException(roundId)
    if (round.closed) throw RoundClosedException(roundId)
    if (round.content?.formatData?.answerable() == false) throw RoundClosedException(roundId)
    if (!round.acceptsAnswers()) throw RoundExpiredException(roundId)
    return round
  }

  /** An abstention is an answer row too, so a user who answered cannot abstain and vice versa. */
  private fun requireNoAnswer(roundId: UUID) {
    val userId = auditorAware.currentAuditor.orElseThrow()
    if (answerRepository.findByRoundIdAndAuditMetadataCreatedBy(roundId, userId) != null) {
      throw AnswerAlreadyExistsException(roundId)
    }
  }

  /**
   * Concurrent submissions can both pass [requireNoAnswer], so the unique index on the round and
   * the user has the last word and its violation is reported like the check's.
   */
  private fun store(answer: Answer): Answer =
      try {
        answerRepository.saveAndFlush(answer)
      } catch (e: DataIntegrityViolationException) {
        throw AnswerAlreadyExistsException(answer.round?.id, e)
      }

  private fun publishAnswerChanged(round: Round) {
    val content = round.content!!
    roundStatsEventPublisher.publishAnswerChanged(
        AnswerChangedEvent(
            roundId = round.id!!,
            contentId = content.id!!,
            questionnaireId = content.questionnaire!!.id!!,
        ))
  }

  /** The worth of a fully correct answer to the content. */
  private fun basePoints(content: Content): Int =
      content.points ?: Content.calculateBasePoints(content.weight)

  /**
   * Both the correctness test and any format-specific answer validation belong to the content's
   * format data, so this path is the same for every format.
   */
  @Transactional
  fun createAnswer(roundId: UUID, formatData: AnswerFormatData): Answer {
    val now = Instant.now()
    val round = findAnswerableRound(roundId)
    requireNoAnswer(roundId)
    val content = round.content!!
    val contentFormatData = content.formatData!!
    contentFormatData.validateAnswer(formatData)
    val result = contentFormatData.evaluateAnswer(formatData)
    val achievedPoints = contentFormatData.calculateAchievedPoints(formatData, basePoints(content))
    val answer =
        Answer(
            round = round,
            formatData = formatData,
            result = result,
            // An answer which achieved nothing is stored without points rather than with zero.
            points = achievedPoints.takeIf { it > 0 },
            durationMs = round.elapsedAnsweringTime(now),
        )
    val persisted = store(answer)
    publishAnswerChanged(round)
    return persisted
  }

  @Transactional
  fun createAbstention(
      roundId: UUID,
  ): Answer {
    val now = Instant.now()
    val round = findAnswerableRound(roundId)
    requireNoAnswer(roundId)
    val answer =
        Answer(
            round = round,
            result = AnswerResultState.ABSTAINED,
            durationMs = round.elapsedAnsweringTime(now),
        )
    return store(answer)
  }

  @Transactional
  fun deleteByContentId(contentId: UUID): Boolean {
    val rounds = roundRepository.findByContentIdOrderByRoundNo(contentId)
    rounds.forEach { answerRepository.deleteByRoundId(it.id!!) }
    return true
  }

  @Transactional
  fun findQuestionnaireContentAnswerResults(
      questionnaireId: UUID,
      userId: UUID,
      subrange: ScrollSubrange
  ): List<ContentAnswerResult> {
    val questionnaire =
        questionnaireRepository.findByIdOrNull(questionnaireId)
            ?: throw QuestionnaireNotFoundException(questionnaireId)
    val contents =
        contentServiceImpl
            .findByQuestionnaireIdOrderByPosition(
                questionnaireId,
                subrange.position().orElse(ScrollPosition.offset()),
            )
            .filter { contentServiceImpl.isPublished(it, questionnaire) }
            .toList()
    return contents.map {
      val answer: Answer? =
          it.activeRound?.let { activeRound ->
            findByRoundIdAndAuditMetadataCreatedBy(activeRound.id!!, userId)
          }
      ContentAnswerResult(
          contentId = it.id!!,
          result = answer?.result ?: AnswerResultState.UNANSWERED,
          durationMs = answer?.durationMs,
      )
    }
  }

  @Transactional
  fun findQuestionnaireAnswerResults(
      questionnaireId: UUID,
      userId: UUID,
      subrange: ScrollSubrange
  ): QuestionnaireAnswerResult {
    val contents =
        contentServiceImpl
            .findByQuestionnaireIdOrderByPosition(
                questionnaireId,
                subrange.position().orElse(ScrollPosition.offset()),
            )
            .toList()
    var correctAnswerCount = 0
    var scorableAnswerCount = 0
    var achievedScore = 0
    var maxScore = 0
    contents.forEach {
      val contentFormatData = it.formatData!!
      if (contentFormatData.hasCorrectAnswer()) {
        scorableAnswerCount++
        if (it.points != null) maxScore += it.points!!
      }
      val answer: Answer? = findByRoundIdAndAuditMetadataCreatedBy(it.activeRound!!.id!!, userId)
      // The result decides whether an answer counts as correct, not the points: older stored
      // answers carry points of 0 rather than none at all even when they are wrong.
      if (answer?.result == AnswerResultState.CORRECT) correctAnswerCount++
      // The score cannot be derived from the result, since a partially correct answer is worth part
      // of the content while its result is not CORRECT.
      val answerFormatData = answer?.formatData ?: return@forEach
      achievedScore += contentFormatData.calculateAchievedPoints(answerFormatData, basePoints(it))
    }
    return QuestionnaireAnswerResult(
        questionnaireId, correctAnswerCount, scorableAnswerCount, achievedScore, maxScore)
  }

  @Deprecated(
      "Deprecated by base implementation", replaceWith = ReplaceWith("deleteAllInBatch(entities)"))
  override fun deleteInBatch(entities: Iterable<Answer>) = answerRepository.deleteInBatch(entities)
}
