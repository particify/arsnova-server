/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.questionnaire

import net.particify.arsnova.core4.questionnaire.content.BinaryContentFormatData
import net.particify.arsnova.core4.questionnaire.content.ChoiceContentFormatData
import net.particify.arsnova.core4.questionnaire.content.ContentFormatData
import net.particify.arsnova.core4.questionnaire.content.FlashcardContentFormatData
import net.particify.arsnova.core4.questionnaire.content.NumericContentFormatData
import net.particify.arsnova.core4.questionnaire.content.PrioritizationContentFormatData
import net.particify.arsnova.core4.questionnaire.content.ScaleContentFormatData
import net.particify.arsnova.core4.questionnaire.content.ShortAnswerContentFormatData
import net.particify.arsnova.core4.questionnaire.content.SlideContentFormatData
import net.particify.arsnova.core4.questionnaire.content.SortContentFormatData
import net.particify.arsnova.core4.questionnaire.content.TextContentFormatData
import net.particify.arsnova.core4.questionnaire.content.WordcloudContentFormatData
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class QuestionnaireTypeTest {
  private val scoredChoice = ChoiceContentFormatData(listOf("A", "B"), listOf(0), false)
  private val unscoredChoice = ChoiceContentFormatData(listOf("A", "B"), null, false)
  private val flashcard = FlashcardContentFormatData("Back")
  private val prioritization = PrioritizationContentFormatData(listOf("A", "B"))
  private val scale = ScaleContentFormatData(ScaleContentFormatData.Template.AGREEMENT, true)
  private val shortAnswer = ShortAnswerContentFormatData(listOf("Paris"))
  private val slide = SlideContentFormatData()
  private val sort = SortContentFormatData(listOf("A", "B"))
  private val text = TextContentFormatData(false)
  private val wordcloud = WordcloudContentFormatData()

  private val allFormats: List<ContentFormatData> =
      listOf(
          BinaryContentFormatData(true),
          BinaryContentFormatData(null),
          flashcard,
          NumericContentFormatData(0.0, 10.0, 5.0),
          NumericContentFormatData(0.0, 10.0),
          prioritization,
          scale,
          scoredChoice,
          shortAnswer,
          slide,
          sort,
          text,
          unscoredChoice,
          wordcloud,
      )

  private fun accepted(type: QuestionnaireType) = allFormats.filter { type.accepts(it) }

  @Test
  fun shouldAcceptEveryFormatInMixedQuestionnaire() {
    Assertions.assertEquals(allFormats, accepted(QuestionnaireType.MIXED))
  }

  @Test
  fun shouldAcceptOnlyFlashcardsAndSlidesInFlashcardQuestionnaire() {
    Assertions.assertEquals(listOf(flashcard, slide), accepted(QuestionnaireType.FLASHCARDS))
  }

  @Test
  fun shouldAcceptOnlyScorableFormatsAndSlidesInQuiz() {
    Assertions.assertEquals(
        listOf(
            BinaryContentFormatData(true),
            NumericContentFormatData(0.0, 10.0, 5.0),
            scoredChoice,
            shortAnswer,
            slide,
            sort),
        accepted(QuestionnaireType.QUIZ))
  }

  @Test
  fun shouldAcceptOnlyUnscoredFormatsAndSlidesInSurvey() {
    Assertions.assertEquals(
        listOf(
            BinaryContentFormatData(null),
            NumericContentFormatData(0.0, 10.0),
            prioritization,
            scale,
            slide,
            text,
            unscoredChoice,
            wordcloud),
        accepted(QuestionnaireType.SURVEY))
  }
}
