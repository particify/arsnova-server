/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.answer

import jakarta.transaction.Transactional
import java.util.Locale
import java.util.UUID
import net.particify.arsnova.core4.questionnaire.content.ContentRepository
import net.particify.arsnova.core4.questionnaire.exception.ContentNotFoundException
import net.particify.arsnova.core4.questionnaire.exception.QuestionnaireNotFoundException
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnaireRepository
import net.particify.arsnova.core4.room.RoomUserAliasService
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class LeaderboardServiceImpl(
    private val answerRepository: AnswerRepository,
    private val contentRepository: ContentRepository,
    private val questionnaireRepository: QuestionnaireRepository,
    private val roomUserAliasService: RoomUserAliasService
) {
  companion object {
    const val DEFAULT_ENTRY_LIMIT = 100
    const val MAX_ENTRY_LIMIT = 1000

    /**
     * Places the scores in descending order. Equal scores share a position and the next distinct
     * score continues after all of them, so a tie of two is followed by position 3. Ties are broken
     * by user ID to keep the order of a page stable across recomputations.
     */
    fun rankByScore(scores: Map<UUID, Int>): List<RankedScore> {
      var position = 0
      var previousScore: Int? = null
      return scores
          .toList()
          .sortedWith(compareByDescending<Pair<UUID, Int>> { it.second }.thenBy { it.first })
          .mapIndexed { index, (userId, score) ->
            if (score != previousScore) {
              position = index + 1
              previousScore = score
            }
            RankedScore(userId, score, position)
          }
    }
  }

  /**
   * Builds the questionnaire's ranking, ordered by score. Passing a [contentId] adds each user's
   * result for that content, which is what the view shown after answering needs. The result is
   * neither localized nor cut to size; both are per-reader and are applied by [toPage].
   */
  @Transactional
  fun findLeaderboardByQuestionnaireId(
      questionnaireId: UUID,
      contentId: UUID?
  ): List<RawLeaderboardEntry> {
    val questionnaire =
        questionnaireRepository.findByIdOrNull(questionnaireId)
            ?: throw QuestionnaireNotFoundException(questionnaireId)
    val aliases = roomUserAliasService.findAliasMappingsByRoomId(questionnaire.roomId!!)
    val currentResults = if (contentId != null) findCurrentResults(contentId) else emptyMap()
    return rankByScore(findScores(questionnaireId)).map {
      RawLeaderboardEntry(
          userId = it.userId,
          alias = aliases[it.userId],
          score = it.score,
          position = it.position,
          currentResult = currentResults[it.userId])
    }
  }

  /**
   * Cuts the ranking down to [limit] entries and localizes their aliases for a single reader. A
   * room can hold a four-digit number of participants, so the full ranking is not something each
   * one of them should be sent.
   */
  fun toPage(
      entries: List<RawLeaderboardEntry>,
      userId: UUID?,
      limit: Int?,
      locale: Locale
  ): LeaderboardPage {
    val entryLimit = (limit ?: DEFAULT_ENTRY_LIMIT).coerceIn(0, MAX_ENTRY_LIMIT)
    val ownEntry =
        userId?.let { id -> entries.asSequence().drop(entryLimit).firstOrNull { it.userId == id } }
    return LeaderboardPage(
        entries = entries.take(entryLimit).map { localize(it, locale) },
        ownEntry = ownEntry?.let { localize(it, locale) },
        totalCount = entries.size)
  }

  private fun localize(entry: RawLeaderboardEntry, locale: Locale): LeaderboardEntry =
      LeaderboardEntry(
          userAlias = entry.alias?.let { roomUserAliasService.localize(it, locale) },
          score = entry.score,
          position = entry.position,
          currentResult = entry.currentResult)

  private fun findScores(questionnaireId: UUID): Map<UUID, Int> {
    return answerRepository.sumPointsByQuestionnaireIdGroupByCreatedBy(questionnaireId).associate {
      Pair(it.get(0, UUID::class.java), (it.get(1, Long::class.java) ?: 0L).toInt())
    }
  }

  private fun findCurrentResults(contentId: UUID): Map<UUID, LeaderboardCurrentResult> {
    val content =
        contentRepository.findByIdOrNull(contentId) ?: throw ContentNotFoundException(contentId)
    val roundId = content.activeRound?.id ?: return emptyMap()
    return answerRepository
        .findByRoundId(roundId)
        .filter { it.auditMetadata.createdBy != null }
        .associate {
          it.auditMetadata.createdBy!! to
              LeaderboardCurrentResult(
                  points = it.points ?: 0,
                  durationMs = it.durationMs ?: 0,
                  correct = it.result == AnswerResultState.CORRECT)
        }
  }
}
