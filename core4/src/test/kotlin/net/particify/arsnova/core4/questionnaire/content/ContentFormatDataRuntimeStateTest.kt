/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.content

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class ContentFormatDataRuntimeStateTest {
  @Test
  fun shouldDropBannedKeywordsButKeepTheWordLimit() {
    val stored = WordcloudContentFormatData(5)
    stored.addBannedKeyword("spam")
    val reset = stored.withoutRuntimeState() as WordcloudContentFormatData
    Assertions.assertEquals(listOf<String>(), reset.bannedKeywords)
    Assertions.assertEquals(5, reset.maxAnswers)
    Assertions.assertEquals(listOf("spam"), stored.bannedKeywords)
  }

  @Test
  fun shouldDropBannedKeywordsButKeepTheCorrectTerms() {
    val stored = ShortAnswerContentFormatData(listOf("Berlin"))
    stored.addBannedKeyword("spam")
    val reset = stored.withoutRuntimeState() as ShortAnswerContentFormatData
    Assertions.assertEquals(listOf<String>(), reset.bannedKeywords)
    Assertions.assertEquals(listOf("Berlin"), reset.correctTerms)
    Assertions.assertEquals(listOf("spam"), stored.bannedKeywords)
  }

  @Test
  fun shouldUnpublishTextAnswers() {
    Assertions.assertEquals(
        TextContentFormatData(false), TextContentFormatData(true).withoutRuntimeState())
  }

  @Test
  fun shouldKeepFormatsWithoutRuntimeStateAsTheyAre() {
    listOf(
            BinaryContentFormatData(true),
            ChoiceContentFormatData(listOf("A", "B"), listOf(1), false),
            FlashcardContentFormatData("Back"),
            NumericContentFormatData(0.0, 10.0, 5.0, 1.0),
            PrioritizationContentFormatData(listOf("A", "B")),
            ScaleContentFormatData(ScaleContentFormatData.Template.AGREEMENT, true),
            SlideContentFormatData(),
            SortContentFormatData(listOf("B", "A"), listOf(1, 0)))
        .forEach { Assertions.assertSame(it, it.withoutRuntimeState(), it.format) }
  }
}
