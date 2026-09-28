/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.answer

import net.particify.arsnova.core4.questionnaire.content.Content
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class CompetitivePointsTest {
  companion object {
    private const val DURATION_SECONDS = 30
    private const val ACHIEVED_POINTS = 100
  }

  private val timedContent = Content(duration = DURATION_SECONDS, points = ACHIEVED_POINTS)

  @Test
  fun shouldDoublePointsForAnInstantAnswer() {
    Assertions.assertEquals(200, timedContent.calculateCompetitivePoints(ACHIEVED_POINTS, 0))
  }

  @Test
  fun shouldKeepPointsForAnAnswerAtHalfTime() {
    Assertions.assertEquals(100, timedContent.calculateCompetitivePoints(ACHIEVED_POINTS, 15000))
  }

  @Test
  fun shouldRemovePointsForAnAnswerAtTheBuzzer() {
    Assertions.assertEquals(0, timedContent.calculateCompetitivePoints(ACHIEVED_POINTS, 30000))
  }

  @Test
  fun shouldWeightPointsByRemainingTime() {
    Assertions.assertEquals(67, timedContent.calculateCompetitivePoints(ACHIEVED_POINTS, 20000))
  }

  @Test
  fun shouldKeepPointsForContentsWithoutDuration() {
    val content = Content(points = ACHIEVED_POINTS)
    Assertions.assertEquals(
        ACHIEVED_POINTS, content.calculateCompetitivePoints(ACHIEVED_POINTS, null))
    Assertions.assertEquals(ACHIEVED_POINTS, content.calculateCompetitivePoints(ACHIEVED_POINTS, 0))
  }

  @Test
  fun shouldKeepPointsWhenAnsweringWasNotTimed() {
    Assertions.assertEquals(
        ACHIEVED_POINTS, timedContent.calculateCompetitivePoints(ACHIEVED_POINTS, null))
  }

  @Test
  fun shouldScoreAContentOfDefaultWeight() {
    val content = Content(duration = DURATION_SECONDS)
    val basePoints = Content.calculateBasePoints(content.weight)
    Assertions.assertEquals(500, basePoints)
    Assertions.assertEquals(1000, content.calculateCompetitivePoints(basePoints, 0))
  }

  @Test
  fun shouldScoreAContentByItsWeight() {
    val content = Content(duration = DURATION_SECONDS, weight = 2)
    val basePoints = Content.calculateBasePoints(content.weight)
    Assertions.assertEquals(1000, basePoints)
    Assertions.assertEquals(2000, content.calculateCompetitivePoints(basePoints, 0))
  }

  @Test
  fun shouldFallBackToASingleWeight() {
    Assertions.assertEquals(500, Content.calculateBasePoints(null))
  }
}
