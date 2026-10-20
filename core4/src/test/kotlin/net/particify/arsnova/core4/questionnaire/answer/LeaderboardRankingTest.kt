/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.answer

import java.util.UUID
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

private val FIRST = UUID.fromString("00000000-0000-0000-0000-000000000001")
private val SECOND = UUID.fromString("00000000-0000-0000-0000-000000000002")
private val THIRD = UUID.fromString("00000000-0000-0000-0000-000000000003")
private val FOURTH = UUID.fromString("00000000-0000-0000-0000-000000000004")

class LeaderboardRankingTest {
  private fun rank(scores: Map<UUID, Int>) =
      LeaderboardServiceImpl.rankByScore(scores).associate { it.userId to it.position }

  @Test
  fun shouldOrderByDescendingScore() {
    val ranking = LeaderboardServiceImpl.rankByScore(mapOf(FIRST to 100, SECOND to 300))
    Assertions.assertEquals(listOf(SECOND, FIRST), ranking.map { it.userId })
    Assertions.assertEquals(listOf(1, 2), ranking.map { it.position })
  }

  @Test
  fun shouldShareAPositionForEqualScores() {
    val positions = rank(mapOf(FIRST to 300, SECOND to 300, THIRD to 300))
    Assertions.assertEquals(1, positions[FIRST])
    Assertions.assertEquals(1, positions[SECOND])
    Assertions.assertEquals(1, positions[THIRD])
  }

  @Test
  fun shouldContinueAfterATieWithTheNextDistinctScore() {
    val positions = rank(mapOf(FIRST to 300, SECOND to 300, THIRD to 200, FOURTH to 100))
    Assertions.assertEquals(1, positions[FIRST])
    Assertions.assertEquals(1, positions[SECOND])
    Assertions.assertEquals(3, positions[THIRD])
    Assertions.assertEquals(4, positions[FOURTH])
  }

  @Test
  fun shouldContinueAfterATieBelowTheLead() {
    val positions = rank(mapOf(FIRST to 500, SECOND to 300, THIRD to 300, FOURTH to 100))
    Assertions.assertEquals(1, positions[FIRST])
    Assertions.assertEquals(2, positions[SECOND])
    Assertions.assertEquals(2, positions[THIRD])
    Assertions.assertEquals(4, positions[FOURTH])
  }

  @Test
  fun shouldOrderTiedEntriesDeterministically() {
    val ranking = LeaderboardServiceImpl.rankByScore(mapOf(SECOND to 300, FIRST to 300))
    Assertions.assertEquals(listOf(FIRST, SECOND), ranking.map { it.userId })
  }

  @Test
  fun shouldRankAnEmptyLeaderboard() {
    Assertions.assertEquals(emptyList<RankedScore>(), LeaderboardServiceImpl.rankByScore(mapOf()))
  }
}
