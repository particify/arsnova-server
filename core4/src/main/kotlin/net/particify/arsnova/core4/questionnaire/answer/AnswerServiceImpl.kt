/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.answer

import jakarta.transaction.Transactional
import java.time.Instant
import java.util.UUID
import net.particify.arsnova.core4.questionnaire.content.Content
import net.particify.arsnova.core4.questionnaire.content.ContentServiceImpl
import net.particify.arsnova.core4.questionnaire.exception.AbstentionsNotAllowedException
import net.particify.arsnova.core4.questionnaire.exception.AnswerAlreadyExistsException
import net.particify.arsnova.core4.questionnaire.exception.ContentNotFoundException
import net.particify.arsnova.core4.questionnaire.exception.QuestionnaireNotFoundException
import net.particify.arsnova.core4.questionnaire.exception.RoundClosedException
import net.particify.arsnova.core4.questionnaire.exception.RoundExpiredException
import net.particify.arsnova.core4.questionnaire.exception.RoundNotFoundException
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnaireRepository
import net.particify.arsnova.core4.questionnaire.round.Round
import net.particify.arsnova.core4.questionnaire.round.RoundRepository
import net.particify.arsnova.core4.questionnaire.round.RoundServiceImpl
import net.particify.arsnova.core4.questionnaire.round.api.AnswerChangedEvent
import net.particify.arsnova.core4.questionnaire.round.api.RoundStatsEventPublisher
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.AuditorAware
import org.springframework.data.domain.ScrollPosition
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class AnswerServiceImpl(
    private val answerRepository: AnswerRepository,
    private val roundRepository: RoundRepository,
    private val contentServiceImpl: ContentServiceImpl,
    private val roundStatsEventPublisher: RoundStatsEventPublisher,
    private val questionnaireRepository: QuestionnaireRepository,
    private val auditorAware: AuditorAware<UUID>,
    private val roundService: RoundServiceImpl,
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

  /** The points an answer given at [at] earns; none once the round's countdown ran out. */
  private fun achievedPoints(
      round: Round,
      content: Content,
      formatData: AnswerFormatData,
      at: Instant?
  ): Int {
    if (at != null && !round.scoresAnswerAt(at)) return 0
    return content.formatData!!.calculateAchievedPoints(formatData, basePoints(content))
  }

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
    val achievedPoints = achievedPoints(round, content, formatData, now)
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
    val content = round.content!!
    if (!content.abstentionsAllowed) throw AbstentionsNotAllowedException(content.id)
    requireNoAnswer(roundId)
    val answer =
        Answer(
            round = round,
            result = AnswerResultState.ABSTAINED,
            durationMs = round.elapsedAnsweringTime(now),
        )
    val persisted = store(answer)
    publishAnswerChanged(round)
    return persisted
  }

  /** Deleting the answers starts the content over, so its rounds go with them. */
  @Transactional
  fun deleteByContentId(contentId: UUID): Boolean {
    val content =
        contentServiceImpl.findByIdOrNull(contentId) ?: throw ContentNotFoundException(contentId)
    roundService.resetRounds(content)
    return true
  }

  @Transactional
  fun findQuestionnaireContentAnswerResults(
      questionnaireId: UUID,
      userId: UUID
  ): List<ContentAnswerResult> {
    val questionnaire =
        questionnaireRepository.findByIdOrNull(questionnaireId)
            ?: throw QuestionnaireNotFoundException(questionnaireId)
    val contents =
        contentServiceImpl
            .findByQuestionnaireIdOrderByPosition(questionnaireId, ScrollPosition.offset())
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

  /** Only released contents count, as the caller can neither see nor answer the others. */
  @Transactional
  fun findQuestionnaireAnswerResults(
      questionnaireId: UUID,
      userId: UUID
  ): QuestionnaireAnswerResult {
    val questionnaire =
        questionnaireRepository.findByIdOrNull(questionnaireId)
            ?: throw QuestionnaireNotFoundException(questionnaireId)
    val contents =
        contentServiceImpl
            .findByQuestionnaireIdOrderByPosition(questionnaireId, ScrollPosition.offset())
            .filter { contentServiceImpl.isPublished(it, questionnaire) }
            .toList()
    val roundIds = contents.mapNotNull { it.activeRound?.id }
    val answersByRoundId =
        answerRepository
            .findByRoundIdInAndAuditMetadataCreatedBy(roundIds, userId)
            .mapNotNull { answer -> answer.round?.id?.let { it to answer } }
            .toMap()
    val result = QuestionnaireAnswerResult(questionnaireId, 0, 0, 0, 0)
    contents.forEach { content ->
      addToResult(result, content, content.activeRound?.id?.let { answersByRoundId[it] })
    }
    return result
  }

  private fun addToResult(result: QuestionnaireAnswerResult, content: Content, answer: Answer?) {
    if (content.formatData!!.hasCorrectAnswer()) {
      result.scorableContentCount++
      content.points?.let { result.maxScore += it }
    }
    // A content without an active round has never been answered.
    val round = content.activeRound
    if (answer == null || round == null) return
    // The result decides whether an answer counts as correct, not the points: older stored answers
    // carry points of 0 rather than none at all even when they are wrong.
    if (answer.result == AnswerResultState.CORRECT) result.correctAnswerCount++
    // The score cannot be derived from the result, since a partially correct answer is worth part
    // of the content while its result is not CORRECT.
    val answerFormatData = answer.formatData ?: return
    result.achievedScore +=
        achievedPoints(round, content, answerFormatData, answer.auditMetadata.createdAt)
  }

  @Deprecated(
      "Deprecated by base implementation", replaceWith = ReplaceWith("deleteAllInBatch(entities)"))
  override fun deleteInBatch(entities: Iterable<Answer>) = answerRepository.deleteInBatch(entities)
}
