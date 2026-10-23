/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire

import java.util.UUID
import net.particify.arsnova.core4.TestcontainersConfiguration
import net.particify.arsnova.core4.common.exception.InvalidInputException
import net.particify.arsnova.core4.questionnaire.answer.Answer
import net.particify.arsnova.core4.questionnaire.answer.AnswerRepository
import net.particify.arsnova.core4.questionnaire.answer.AnswerResultState
import net.particify.arsnova.core4.questionnaire.content.ChoiceContentFormatData
import net.particify.arsnova.core4.questionnaire.content.Content
import net.particify.arsnova.core4.questionnaire.content.ContentServiceImpl
import net.particify.arsnova.core4.questionnaire.content.NumericContentFormatData
import net.particify.arsnova.core4.questionnaire.content.api.ContentAnswersFieldController
import net.particify.arsnova.core4.questionnaire.exception.ContentFormatDataInvalidException
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

/** Account and room from the `dev` Liquibase fixtures; the account owns the room. */
private val USER_ID = UUID.fromString("9c778494-9b52-424d-8c7f-f4936629facb")
private val ROOM_ID = UUID.fromString("3f9cecb5-6dbf-45cc-b630-4af700d474b6")

/** Any answer in any round counts, hidden ones included, as it does for the edit lock. */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration::class)
class ContentHasAnswersTests {
  @Autowired lateinit var answerRepository: AnswerRepository
  @Autowired lateinit var contentAnswersFieldController: ContentAnswersFieldController
  @Autowired lateinit var contentService: ContentServiceImpl
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

  private fun createChoiceContent(questionnaireId: UUID): Content =
      contentService.create(
          questionnaireId,
          "Choice content",
          ChoiceContentFormatData(listOf("A", "B"), listOf(0), false),
          null,
          true,
          null)

  @Test
  fun shouldCountAHiddenAnswerInAClosedEarlierRound() {
    val questionnaire =
        questionnaireService.create(
            ROOM_ID, "Has answers ${UUID.randomUUID()}", QuestionnaireType.SURVEY)
    val answered = createChoiceContent(questionnaire.id!!)
    val unanswered = createChoiceContent(questionnaire.id!!)
    answerRepository.saveAndFlush(
        Answer(round = answered.activeRound, result = AnswerResultState.NEUTRAL, hidden = true))
    val secondRound = roundService.startNewRound(answered.id!!)
    Assertions.assertEquals(2, secondRound.roundNo)

    val result = contentAnswersFieldController.hasAnswers(listOf(answered, unanswered))

    Assertions.assertEquals(true, result[answered])
    Assertions.assertEquals(false, result[unanswered])
  }

  /** What an answer was given under is locked, while the labels of the options stay editable. */
  @Test
  fun shouldLockTheSettingsOfAnAnsweredChoiceContent() {
    val questionnaire =
        questionnaireService.create(ROOM_ID, "Lock ${UUID.randomUUID()}", QuestionnaireType.SURVEY)
    val choice = createChoiceContent(questionnaire.id!!)
    answer(choice)

    val relabelled =
        contentService.update(
            choice.id!!,
            "Choice content",
            ChoiceContentFormatData(listOf("A", "Bee"), listOf(0), false),
            null,
            true,
            null)
    Assertions.assertEquals(
        listOf("A", "Bee"), (relabelled.formatData as ChoiceContentFormatData).options)
    Assertions.assertThrows(ContentFormatDataInvalidException::class.java) {
      contentService.update(
          choice.id!!,
          "Choice content",
          ChoiceContentFormatData(listOf("Bee", "A"), listOf(0), false),
          null,
          true,
          null)
    }
  }

  @Test
  fun shouldLockTheRangeOfAnAnsweredNumericContent() {
    val questionnaire =
        questionnaireService.create(ROOM_ID, "Lock ${UUID.randomUUID()}", QuestionnaireType.SURVEY)
    val numeric =
        contentService.create(
            questionnaire.id!!, "Numeric", NumericContentFormatData(0.0, 10.0), null, true, null)
    answer(numeric)

    val renamed =
        contentService.update(
            numeric.id!!, "Renamed", NumericContentFormatData(0.0, 10.0), null, true, null)
    Assertions.assertEquals("Renamed", renamed.body)
    listOf(NumericContentFormatData(-10.0, 20.0), NumericContentFormatData(0.0, 5.0)).forEach {
      Assertions.assertThrows(
          ContentFormatDataInvalidException::class.java,
          { contentService.update(numeric.id!!, "Numeric", it, null, true, null) },
          "$it")
    }
  }

  @Test
  fun shouldLockTheWeightOfAnAnsweredContent() {
    val questionnaire =
        questionnaireService.create(ROOM_ID, "Lock ${UUID.randomUUID()}", QuestionnaireType.SURVEY)
    val choice = createChoiceContent(questionnaire.id!!)
    answer(choice)

    Assertions.assertThrows(InvalidInputException::class.java) {
      contentService.update(
          choice.id!!,
          "Choice content",
          ChoiceContentFormatData(listOf("A", "B"), listOf(0), false),
          null,
          true,
          2)
    }
  }

  private fun answer(content: Content) {
    answerRepository.saveAndFlush(
        Answer(round = content.activeRound, result = AnswerResultState.NEUTRAL))
  }
}
