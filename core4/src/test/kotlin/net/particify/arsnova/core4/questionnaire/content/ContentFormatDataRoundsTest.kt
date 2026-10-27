/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.content

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class ContentFormatDataRoundsTest {
  @Test
  fun shouldNotSupportRoundsByDefault() {
    val formatData =
        object : ContentFormatData {
          override val format = "UNSUPPORTED"

          override fun withoutRuntimeState() = this
        }
    Assertions.assertFalse(formatData.supportsRounds())
  }

  @Test
  fun shouldSupportRoundsForChoice() {
    Assertions.assertTrue(ChoiceContentFormatData(listOf("A", "B"), null, false).supportsRounds())
  }

  @Test
  fun shouldSupportRoundsForWordcloud() {
    Assertions.assertTrue(WordcloudContentFormatData().supportsRounds())
  }

  @Test
  fun shouldSupportRoundsForNumeric() {
    Assertions.assertTrue(NumericContentFormatData(0.0, 10.0).supportsRounds())
  }

  @Test
  fun shouldSupportRoundsForScale() {
    Assertions.assertTrue(
        ScaleContentFormatData(ScaleContentFormatData.Template.AGREEMENT, true).supportsRounds())
  }

  @Test
  fun shouldSupportRoundsForBinary() {
    Assertions.assertTrue(BinaryContentFormatData().supportsRounds())
  }

  /**
   * A deliberate product decision, shared with the web client, rather than a default to inherit.
   */
  @Test
  fun shouldNotSupportRoundsForShortAnswerSortPrioritizationAndText() {
    listOf(
            ShortAnswerContentFormatData(listOf("Paris")),
            SortContentFormatData(listOf("A", "B"), null),
            PrioritizationContentFormatData(listOf("A", "B")),
            TextContentFormatData(false))
        .forEach { Assertions.assertFalse(it.supportsRounds(), it.format) }
  }
}
