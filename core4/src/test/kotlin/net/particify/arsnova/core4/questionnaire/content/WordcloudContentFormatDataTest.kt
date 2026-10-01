/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.content

import net.particify.arsnova.core4.questionnaire.answer.ChoiceAnswerFormatData
import net.particify.arsnova.core4.questionnaire.answer.WordcloudAnswerFormatData
import net.particify.arsnova.core4.questionnaire.exception.ContentFormatDataInvalidException
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class WordcloudContentFormatDataTest {
  @Test
  fun shouldStartWithoutBannedKeywords() {
    val formatData = WordcloudContentFormatData()
    Assertions.assertEquals(WordcloudContentFormatData.MAX_ANSWERS_DEFAULT, formatData.maxAnswers)
    Assertions.assertEquals(listOf<String>(), formatData.bannedKeywords)
  }

  @Test
  fun shouldAppendBannedKeywordsInOrder() {
    val formatData = WordcloudContentFormatData()
    formatData.addBannedKeyword("spam")
    formatData.addBannedKeyword("eggs")
    Assertions.assertEquals(listOf("spam", "eggs"), formatData.bannedKeywords)
  }

  @Test
  fun shouldResetBannedKeywordsAndAcceptNewOnesAfterwards() {
    val formatData = WordcloudContentFormatData()
    formatData.addBannedKeyword("spam")
    formatData.resetBannedKeywords()
    Assertions.assertEquals(listOf<String>(), formatData.bannedKeywords)
    formatData.addBannedKeyword("eggs")
    Assertions.assertEquals(listOf("eggs"), formatData.bannedKeywords)
  }

  @Test
  fun shouldIgnoreVariantOfBannedKeyword() {
    val formatData = WordcloudContentFormatData()
    formatData.addBannedKeyword("spam")
    formatData.addBannedKeyword(" Spam! ")
    Assertions.assertEquals(listOf("spam"), formatData.bannedKeywords)
  }

  @Test
  fun shouldCarryBannedKeywordsOverOnUpdate() {
    val current = WordcloudContentFormatData()
    current.addBannedKeyword("spam")
    val merged = WordcloudContentFormatData(5).mergeWith(current) as WordcloudContentFormatData
    Assertions.assertEquals(5, merged.maxAnswers)
    Assertions.assertEquals(listOf("spam"), merged.bannedKeywords)
    current.addBannedKeyword("eggs")
    Assertions.assertEquals(listOf("spam"), merged.bannedKeywords)
  }

  @Test
  fun shouldStartWithoutBannedKeywordsWhenChangedFromOtherFormat() {
    val merged =
        WordcloudContentFormatData(5).mergeWith(ChoiceContentFormatData(listOf("A"), null, false))
            as WordcloudContentFormatData
    Assertions.assertEquals(listOf<String>(), merged.bannedKeywords)
  }

  @Test
  fun shouldAcceptAnswerUpToMaxAnswersWordsOfMaximumLength() {
    val longest = "x".repeat(WordcloudContentFormatData.MAX_WORD_LENGTH)
    WordcloudContentFormatData(2).validateAnswer(WordcloudAnswerFormatData(listOf("a", longest)))
  }

  @Test
  fun shouldRejectMoreWordsThanMaxAnswers() {
    assertRejected(WordcloudContentFormatData(2), listOf("a", "b", "c"))
  }

  @Test
  fun shouldRejectBlankWord() {
    listOf("", " ", "\t").forEach { assertRejected(WordcloudContentFormatData(), listOf("a", it)) }
  }

  @Test
  fun shouldRejectWordAboveMaximumLength() {
    val tooLong = "x".repeat(WordcloudContentFormatData.MAX_WORD_LENGTH + 1)
    assertRejected(WordcloudContentFormatData(), listOf(tooLong))
  }

  @Test
  fun shouldRejectAnswerOfOtherFormat() {
    Assertions.assertThrows(ContentFormatDataInvalidException::class.java) {
      WordcloudContentFormatData().validateAnswer(ChoiceAnswerFormatData(listOf(0)))
    }
  }

  private fun assertRejected(formatData: WordcloudContentFormatData, words: List<String>) {
    Assertions.assertThrows(ContentFormatDataInvalidException::class.java) {
      formatData.validateAnswer(WordcloudAnswerFormatData(words))
    }
  }
}
