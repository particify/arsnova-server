/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.answer

import jakarta.transaction.Transactional
import java.util.Locale
import java.util.UUID
import net.particify.arsnova.core4.questionnaire.content.Content
import net.particify.arsnova.core4.questionnaire.content.ContentRepository
import net.particify.arsnova.core4.questionnaire.content.SolutionVisibility
import net.particify.arsnova.core4.questionnaire.exception.ContentNotFoundException
import net.particify.arsnova.core4.questionnaire.exception.QuestionnaireNotFoundException
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnaireRepository
import net.particify.arsnova.core4.room.RoomUserAliasService
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.access.PermissionEvaluator
import org.springframework.stereotype.Service

@Service
class LeaderboardServiceImpl(
    private val answerRepository: AnswerRepository,
    private val contentRepository: ContentRepository,
    private val questionnaireRepository: QuestionnaireRepository,
    private val roomUserAliasService: RoomUserAliasService,
    private val permissionEvaluator: PermissionEvaluator
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
    contentId?.let { requireContentOfQuestionnaire(questionnaireId, it) }
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
          currentResult = currentResults[it.userId],
          solutionPublished = questionnaire.correctOptionsPublished == true)
    }
  }

  /**
   * Whether the current user may see how the current results were scored before the solution is
   * published, which is decided by [SolutionVisibility] as for a single answer. The shared
   * computation and the emissions of a subscription have no reader, so this is asked once up front.
   */
  @Transactional
  fun maySeeUnpublishedResults(questionnaireId: UUID, contentId: UUID?): Boolean {
    if (contentId == null) return false
    val content = requireContentOfQuestionnaire(questionnaireId, contentId)
    return SolutionVisibility.isPrivileged(permissionEvaluator, content)
  }

  /**
   * Cuts the ranking down to [limit] entries and localizes their aliases for a single reader. A
   * room can hold a four-digit number of participants, so the full ranking is not something each
   * one of them should be sent. The current results are withheld as long as the solution is
   * unpublished, unless [maySeeUnpublishedResults] holds for the reader.
   */
  fun toPage(
      entries: List<RawLeaderboardEntry>,
      userId: UUID?,
      limit: Int?,
      locale: Locale,
      maySeeUnpublishedResults: Boolean
  ): LeaderboardPage {
    val entryLimit = (limit ?: DEFAULT_ENTRY_LIMIT).coerceIn(0, MAX_ENTRY_LIMIT)
    val ownEntry =
        userId?.let { id -> entries.asSequence().drop(entryLimit).firstOrNull { it.userId == id } }
    val pageEntry = { entry: RawLeaderboardEntry ->
      toEntry(entry, locale, maySeeUnpublishedResults || entry.solutionPublished)
    }
    return LeaderboardPage(
        entries = entries.take(entryLimit).map(pageEntry),
        ownEntry = ownEntry?.let(pageEntry),
        totalCount = entries.size)
  }

  private fun toEntry(
      entry: RawLeaderboardEntry,
      locale: Locale,
      maySeeResults: Boolean
  ): LeaderboardEntry =
      LeaderboardEntry(
          userAlias = entry.alias?.let { roomUserAliasService.localize(it, locale) },
          score = entry.score,
          position = entry.position,
          currentResult =
              if (maySeeResults) entry.currentResult else entry.currentResult?.withoutCorrectness())

  private fun findScores(questionnaireId: UUID): Map<UUID, Int> {
    return answerRepository.sumPointsByQuestionnaireIdGroupByCreatedBy(questionnaireId).associate {
      Pair(it.get(0, UUID::class.java), (it.get(1, Long::class.java) ?: 0L).toInt())
    }
  }

  /**
   * Fails like an unknown content for a content of another questionnaire. The permission is checked
   * on the questionnaire only, so the content must not be able to reach beyond it, and the error
   * must not reveal that the content exists.
   */
  fun requireContentOfQuestionnaire(questionnaireId: UUID, contentId: UUID): Content =
      contentRepository.findByIdOrNull(contentId)?.takeIf {
        it.questionnaire?.id == questionnaireId
      } ?: throw ContentNotFoundException(contentId)

  private fun findCurrentResults(contentId: UUID): Map<UUID, LeaderboardCurrentResult> {
    val content =
        contentRepository.findByIdOrNull(contentId) ?: throw ContentNotFoundException(contentId)
    val roundId = content.activeRound?.id ?: return emptyMap()
    return answerRepository.findLeaderboardProjectionsByRoundId(roundId).associate {
      it.userId to
          LeaderboardCurrentResult(
              points = it.points ?: 0,
              durationMs = it.durationMs ?: 0,
              correct = it.result == AnswerResultState.CORRECT)
    }
  }
}
