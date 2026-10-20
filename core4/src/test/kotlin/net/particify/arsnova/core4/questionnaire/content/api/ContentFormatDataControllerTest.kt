/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.content.api

import java.util.UUID
import net.particify.arsnova.core4.questionnaire.content.ChoiceContentFormatData
import net.particify.arsnova.core4.questionnaire.content.Content
import net.particify.arsnova.core4.questionnaire.content.SolutionVisibility
import net.particify.arsnova.core4.questionnaire.questionnaire.Questionnaire
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.Mockito

class ContentFormatDataControllerTest {
  private val solutionVisibility = Mockito.mock(SolutionVisibility::class.java)
  private val controller = ContentFormatDataController(solutionVisibility)
  private val stored = ChoiceContentFormatData(listOf("A", "B"), listOf(1), false)
  private val content =
      Content(id = UUID.randomUUID(), questionnaire = Questionnaire(), formatData = stored)

  @Test
  fun shouldServeStoredFormatDataToWhoeverMaySeeTheSolution() {
    Mockito.`when`(solutionVisibility.maySeeSolution(content)).thenReturn(true)
    Assertions.assertSame(stored, controller.formatData(content))
  }

  @Test
  fun shouldWithholdTheSolutionWithoutTouchingTheStoredFormatData() {
    Mockito.`when`(solutionVisibility.maySeeSolution(content)).thenReturn(false)
    Assertions.assertEquals(
        ChoiceContentFormatData(listOf("A", "B"), null, false), controller.formatData(content))
    Assertions.assertEquals(listOf(1), stored.correctOptionIndexes)
    Assertions.assertSame(stored, content.formatData)
  }
}
