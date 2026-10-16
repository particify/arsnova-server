/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.content

import net.particify.arsnova.core4.questionnaire.answer.AnswerResultState
import net.particify.arsnova.core4.questionnaire.answer.ShortAnswerAnswerFormatData
import net.particify.arsnova.core4.questionnaire.answer.TextAnswerFormatData
import net.particify.arsnova.core4.questionnaire.exception.ContentFormatDataInvalidException
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class ShortAnswerContentFormatDataTest {
  private val formatData = ShortAnswerContentFormatData(listOf("Frankfurt am Main", "FFM"))

  private fun assertInvalid(correctTerms: List<String>) {
    Assertions.assertThrows(
        ContentFormatDataInvalidException::class.java,
        { ShortAnswerContentFormatData(correctTerms).validate() },
        "$correctTerms")
  }

  @Test
  fun shouldAcceptTermsWithinTheLimits() {
    ShortAnswerContentFormatData(listOf("A")).validate()
    val maxLength = "x".repeat(ShortAnswerContentFormatData.MAX_TERM_LENGTH)
    ShortAnswerContentFormatData(listOf(maxLength)).validate()
    val maxCount = (1..ShortAnswerContentFormatData.MAX_TERM_COUNT).map { "Term $it" }
    ShortAnswerContentFormatData(maxCount).validate()
  }

  @Test
  fun shouldRejectNoTermsOrTooManyTerms() {
    assertInvalid(listOf())
    assertInvalid((0..ShortAnswerContentFormatData.MAX_TERM_COUNT).map { "Term $it" })
  }

  @Test
  fun shouldRejectBlankOrOverlongTerms() {
    assertInvalid(listOf("A", " "))
    assertInvalid(listOf("x".repeat(ShortAnswerContentFormatData.MAX_TERM_LENGTH + 1)))
  }

  @Test
  fun shouldRejectTermWithoutLettersOrDigits() {
    assertInvalid(listOf("Berlin", "?!"))
  }

  @Test
  fun shouldAcceptAnswerUpToMaximumLength() {
    formatData.validateAnswer(ShortAnswerAnswerFormatData("FFM"))
    val longest = "x".repeat(ShortAnswerContentFormatData.MAX_TERM_LENGTH)
    formatData.validateAnswer(ShortAnswerAnswerFormatData(longest))
  }

  @Test
  fun shouldRejectAnswerWithoutLettersOrDigitsOrAboveMaximumLength() {
    val tooLong = "x".repeat(ShortAnswerContentFormatData.MAX_TERM_LENGTH + 1)
    listOf("", " ", "...", tooLong).forEach {
      Assertions.assertThrows(
          ContentFormatDataInvalidException::class.java,
          { formatData.validateAnswer(ShortAnswerAnswerFormatData(it)) },
          it)
    }
  }

  @Test
  fun shouldRejectTermsRepeatingInNormalizedForm() {
    assertInvalid(listOf("Berlin", "berlin!"))
  }

  @Test
  fun shouldAcceptAnswersMatchingATermInNormalizedForm() {
    listOf("frankfurt am main", "FRANKFURT-AM-MAIN!", " ffm ").forEach {
      Assertions.assertTrue(formatData.isCorrect(it), it)
      Assertions.assertEquals(
          AnswerResultState.CORRECT, formatData.evaluateAnswer(ShortAnswerAnswerFormatData(it)))
    }
  }

  @Test
  fun shouldRejectAnswersMatchingNoTerm() {
    listOf("Frankfurt", "Frankfurt an der Oder").forEach {
      Assertions.assertFalse(formatData.isCorrect(it), it)
      Assertions.assertEquals(
          AnswerResultState.WRONG, formatData.evaluateAnswer(ShortAnswerAnswerFormatData(it)))
    }
  }

  @Test
  fun shouldReportACorrectAnswerOnlyWithTerms() {
    Assertions.assertTrue(formatData.hasCorrectAnswer())
    Assertions.assertFalse(ShortAnswerContentFormatData(listOf()).hasCorrectAnswer())
  }

  @Test
  fun shouldRejectAnswerOfOtherFormat() {
    Assertions.assertThrows(ContentFormatDataInvalidException::class.java) {
      formatData.validateAnswer(TextAnswerFormatData("FFM"))
    }
  }

  @Test
  fun shouldEvaluateStoredAnswerOfOtherFormatAsNeutral() {
    Assertions.assertEquals(
        AnswerResultState.NEUTRAL, formatData.evaluateAnswer(TextAnswerFormatData("FFM")))
  }

  @Test
  fun shouldAllowReorderingAndRespellingTermsOnceAnswered() {
    ShortAnswerContentFormatData(listOf("ffm", "Frankfurt-am-Main")).validateChange(formatData) {
      Assertions.fail("Answers must not be looked up for an unchanged set of terms")
    }
  }

  @Test
  fun shouldLockTheSetOfTermsOnceAnswered() {
    listOf(listOf("Frankfurt am Main"), listOf("Frankfurt am Main", "FFM", "Mainhattan")).forEach {
      Assertions.assertThrows(
          ContentFormatDataInvalidException::class.java,
          { ShortAnswerContentFormatData(it).validateChange(formatData) { true } },
          "$it")
    }
  }

  @Test
  fun shouldAllowChangingTermsWithoutAnswers() {
    ShortAnswerContentFormatData(listOf("Offenbach")).validateChange(formatData) { false }
  }

  @Test
  fun shouldRejectUpdateOfOtherFormat() {
    Assertions.assertThrows(ContentFormatDataInvalidException::class.java) {
      formatData.validateChange(TextContentFormatData(false)) { false }
    }
  }

  @Test
  fun shouldBanEachKeywordOnceByItsNormalizedForm() {
    val formatData = ShortAnswerContentFormatData(listOf("Frankfurt am Main"))
    formatData.addBannedKeyword("Spam!")
    formatData.addBannedKeyword("SPAM")
    formatData.addBannedKeyword("Hanau")
    Assertions.assertEquals(listOf("Spam!", "Hanau"), formatData.bannedKeywords)
  }

  @Test
  fun shouldResetBannedKeywords() {
    val formatData = ShortAnswerContentFormatData(listOf("Frankfurt am Main"))
    formatData.addBannedKeyword("Spam")
    formatData.resetBannedKeywords()
    Assertions.assertEquals(listOf<String>(), formatData.bannedKeywords)
  }

  @Test
  fun shouldKeepBannedKeywordsWhenMergedWithTheCurrentFormatData() {
    val current = ShortAnswerContentFormatData(listOf("Frankfurt am Main"))
    current.addBannedKeyword("Spam")
    val merged = ShortAnswerContentFormatData(listOf("Frankfurt")).mergeWith(current)
    Assertions.assertEquals(
        listOf("Frankfurt"), (merged as ShortAnswerContentFormatData).correctTerms)
    Assertions.assertEquals(listOf("Spam"), merged.bannedKeywords)
    current.addBannedKeyword("Hanau")
    Assertions.assertEquals(listOf("Spam"), merged.bannedKeywords)
  }

  @Test
  fun shouldScoreABannedCorrectTermAsCorrect() {
    formatData.addBannedKeyword("FFM")
    Assertions.assertEquals(
        AnswerResultState.CORRECT, formatData.evaluateAnswer(ShortAnswerAnswerFormatData("ffm")))
  }

  @Test
  fun shouldTellFormatDataApartByTheirBannedKeywords() {
    val banned = ShortAnswerContentFormatData(listOf("Frankfurt am Main"))
    banned.addBannedKeyword("Spam")
    Assertions.assertNotEquals(ShortAnswerContentFormatData(listOf("Frankfurt am Main")), banned)
  }

  @Test
  fun shouldWithholdTheTermsAsAnEmptyList() {
    Assertions.assertEquals(listOf<String>(), formatData.withoutSolution().correctTerms)
    Assertions.assertEquals(listOf("Frankfurt am Main", "FFM"), formatData.correctTerms)
  }
}
