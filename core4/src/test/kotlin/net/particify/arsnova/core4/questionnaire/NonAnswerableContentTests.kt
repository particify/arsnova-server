/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire

import java.util.UUID
import net.particify.arsnova.core4.TestcontainersConfiguration
import net.particify.arsnova.core4.questionnaire.answer.AnswerServiceImpl
import net.particify.arsnova.core4.questionnaire.content.ChoiceContentFormatData
import net.particify.arsnova.core4.questionnaire.content.Content
import net.particify.arsnova.core4.questionnaire.content.ContentServiceImpl
import net.particify.arsnova.core4.questionnaire.content.FlashcardContentFormatData
import net.particify.arsnova.core4.questionnaire.content.SlideContentFormatData
import net.particify.arsnova.core4.questionnaire.exception.ContentFormatDataInvalidException
import net.particify.arsnova.core4.questionnaire.exception.RoundClosedException
import net.particify.arsnova.core4.questionnaire.exception.RoundsNotSupportedException
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnairePublishingServiceImpl
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnaireServiceImpl
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnaireType
import net.particify.arsnova.core4.questionnaire.round.RoundServiceImpl
import net.particify.arsnova.core4.user.UserService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.ActiveProfiles

private val ROOM_ID = UUID.fromString("3f9cecb5-6dbf-45cc-b630-4af700d474b6")
private val USER_ID = UUID.fromString("9c778494-9b52-424d-8c7f-f4936629facb")

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration::class)
class NonAnswerableContentTests {
  @Autowired lateinit var answerService: AnswerServiceImpl
  @Autowired lateinit var contentService: ContentServiceImpl
  @Autowired lateinit var publishingService: QuestionnairePublishingServiceImpl
  @Autowired lateinit var questionnaireService: QuestionnaireServiceImpl
  @Autowired lateinit var roundService: RoundServiceImpl
  @Autowired lateinit var userService: UserService

  @BeforeEach
  fun authenticateUser() {
    val user = userService.loadUserById(USER_ID)!!
    SecurityContextHolder.getContext().authentication =
        UsernamePasswordAuthenticationToken(user, null, user.authorities)
  }

  @AfterEach
  fun clearAuthentication() {
    SecurityContextHolder.clearContext()
  }

  private fun createQuestionnaire(type: QuestionnaireType) =
      questionnaireService.create(ROOM_ID, "Format ${UUID.randomUUID()}", type)

  private fun createFlashcardContent(questionnaireId: UUID, answer: String = "Back"): Content =
      contentService.create(
          questionnaireId, "Front", FlashcardContentFormatData(answer), null, false, null)

  private fun createSlideContent(questionnaireId: UUID): Content =
      contentService.create(questionnaireId, "Slide", SlideContentFormatData(), null, false, null)

  private fun createChoiceContent(questionnaireId: UUID): Content =
      contentService.create(
          questionnaireId,
          "Choice content",
          ChoiceContentFormatData(listOf("A", "B"), listOf(0), false),
          null,
          true,
          null)

  @Test
  fun shouldPersistAndReloadFlashcardFormat() {
    val questionnaire = createQuestionnaire(QuestionnaireType.FLASHCARDS)
    val content = createFlashcardContent(questionnaire.id!!, "Back of card")
    val reloaded = contentService.findById(content.id!!).orElseThrow()
    Assertions.assertEquals("FLASHCARD", reloaded.formatData!!.format)
    Assertions.assertEquals(
        "Back of card", (reloaded.formatData as FlashcardContentFormatData).answer)
  }

  @Test
  fun shouldRejectBlankAnswerOnCreate() {
    val questionnaire = createQuestionnaire(QuestionnaireType.FLASHCARDS)
    Assertions.assertThrows(ContentFormatDataInvalidException::class.java) {
      contentService.create(
          questionnaire.id!!, "Front", FlashcardContentFormatData(" "), null, false, null)
    }
  }

  @Test
  fun shouldRejectOverLengthAnswerOnCreate() {
    val questionnaire = createQuestionnaire(QuestionnaireType.FLASHCARDS)
    val tooLong = "a".repeat(FlashcardContentFormatData.MAX_ANSWER_LENGTH + 1)
    Assertions.assertThrows(ContentFormatDataInvalidException::class.java) {
      contentService.create(
          questionnaire.id!!, "Front", FlashcardContentFormatData(tooLong), null, false, null)
    }
  }

  @Test
  fun shouldRejectBlankAnswerOnUpdate() {
    val questionnaire = createQuestionnaire(QuestionnaireType.FLASHCARDS)
    val content = createFlashcardContent(questionnaire.id!!)
    Assertions.assertThrows(ContentFormatDataInvalidException::class.java) {
      contentService.update(
          content.id!!, "Front", FlashcardContentFormatData(" "), null, false, null)
    }
  }

  @Test
  fun shouldRejectOverLengthAnswerOnUpdate() {
    val questionnaire = createQuestionnaire(QuestionnaireType.FLASHCARDS)
    val content = createFlashcardContent(questionnaire.id!!)
    val tooLong = "a".repeat(FlashcardContentFormatData.MAX_ANSWER_LENGTH + 1)
    Assertions.assertThrows(ContentFormatDataInvalidException::class.java) {
      contentService.update(
          content.id!!, "Front", FlashcardContentFormatData(tooLong), null, false, null)
    }
  }

  @Test
  fun shouldPreserveAnswerWhenDuplicating() {
    val questionnaire = createQuestionnaire(QuestionnaireType.FLASHCARDS)
    val content = createFlashcardContent(questionnaire.id!!, "Back text")
    val duplicated = contentService.duplicate(content)
    Assertions.assertEquals(
        "Back text", (duplicated.formatData as FlashcardContentFormatData).answer)
  }

  @Test
  fun shouldPreserveAnswerWhenCopyingToOtherQuestionnaire() {
    val questionnaire = createQuestionnaire(QuestionnaireType.FLASHCARDS)
    val otherQuestionnaire = createQuestionnaire(QuestionnaireType.FLASHCARDS)
    val content = createFlashcardContent(questionnaire.id!!, "Back text")
    val copied = contentService.duplicate(content, otherQuestionnaire)
    Assertions.assertEquals("Back text", (copied.formatData as FlashcardContentFormatData).answer)
    Assertions.assertEquals(otherQuestionnaire.id, copied.questionnaire!!.id)
  }

  @Test
  fun shouldAdvancePublishedUpToContentWithoutOpeningAnsweringForLiveSlide() {
    val questionnaire = createQuestionnaire(QuestionnaireType.QUIZ)
    val slide = createSlideContent(questionnaire.id!!)
    val updated = publishingService.startLiveContent(slide.id!!)
    Assertions.assertEquals(slide.id, updated.publishedUpToContent!!.id)
    val round = updated.activeRound!!
    Assertions.assertTrue(round.closed)
    Assertions.assertNull(round.answeringEndTime)
  }

  @Test
  fun shouldRejectAbstentionOnSlideRoundInLiveMode() {
    val questionnaire = createQuestionnaire(QuestionnaireType.QUIZ)
    val slide = createSlideContent(questionnaire.id!!)
    val updated = publishingService.startLiveContent(slide.id!!)
    val roundId = updated.activeRound!!.id!!
    Assertions.assertThrows(RoundClosedException::class.java) {
      answerService.createAbstention(roundId)
    }
  }

  @Test
  fun shouldRejectAbstentionOnSlideRoundInAllMode() {
    val questionnaire = createQuestionnaire(QuestionnaireType.SURVEY)
    val slide = createSlideContent(questionnaire.id!!)
    val roundId = slide.activeRound!!.id!!
    Assertions.assertThrows(RoundClosedException::class.java) {
      answerService.createAbstention(roundId)
    }
  }

  @Test
  fun shouldRejectNewRoundOfLiveSlide() {
    val questionnaire = createQuestionnaire(QuestionnaireType.QUIZ)
    val slide = createSlideContent(questionnaire.id!!)
    publishingService.startLiveContent(slide.id!!)
    Assertions.assertThrows(RoundsNotSupportedException::class.java) {
      roundService.startNewRound(slide.id!!)
    }
  }

  @Test
  fun shouldRejectNewRoundOfLiveFlashcard() {
    val questionnaire = createQuestionnaire(QuestionnaireType.QUIZ)
    val flashcard = createFlashcardContent(questionnaire.id!!)
    publishingService.startLiveContent(flashcard.id!!)
    Assertions.assertThrows(RoundsNotSupportedException::class.java) {
      roundService.startNewRound(flashcard.id!!)
    }
  }

  @Test
  fun shouldNotThrowWhenComputingAnswerResultsForMixedQuestionnaire() {
    val questionnaire = createQuestionnaire(QuestionnaireType.MIXED)
    createChoiceContent(questionnaire.id!!)
    createSlideContent(questionnaire.id!!)
    Assertions.assertDoesNotThrow {
      answerService.findQuestionnaireAnswerResults(questionnaire.id!!, USER_ID)
    }
  }
}
