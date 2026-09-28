/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire

import java.util.UUID
import net.particify.arsnova.core4.TestcontainersConfiguration
import net.particify.arsnova.core4.questionnaire.answer.Answer
import net.particify.arsnova.core4.questionnaire.answer.AnswerRepository
import net.particify.arsnova.core4.questionnaire.answer.AnswerResultState
import net.particify.arsnova.core4.questionnaire.answer.AnswerServiceImpl
import net.particify.arsnova.core4.questionnaire.answer.BinaryAnswerFormatData
import net.particify.arsnova.core4.questionnaire.content.BinaryContentFormatData
import net.particify.arsnova.core4.questionnaire.content.Content
import net.particify.arsnova.core4.questionnaire.content.ContentServiceImpl
import net.particify.arsnova.core4.questionnaire.content.TextContentFormatData
import net.particify.arsnova.core4.questionnaire.exception.ContentFormatDataInvalidException
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnaireServiceImpl
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnaireType
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
private val OWNER_ID = UUID.fromString("9c778494-9b52-424d-8c7f-f4936629facb")
private const val DURATION = 30
private const val BODY = "Is Hamburg north of Munich?"
private const val OTHER_BODY = "Is Munich north of Hamburg?"

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration::class)
class BinaryContentTests {
  @Autowired lateinit var answerRepository: AnswerRepository
  @Autowired lateinit var answerService: AnswerServiceImpl
  @Autowired lateinit var contentService: ContentServiceImpl
  @Autowired lateinit var questionnaireService: QuestionnaireServiceImpl
  @Autowired lateinit var userService: UserService

  @BeforeEach
  fun authenticateUser() {
    authenticate(OWNER_ID)
  }

  private fun authenticate(userId: UUID) {
    val user = userService.loadUserById(userId)!!
    SecurityContextHolder.getContext().authentication =
        UsernamePasswordAuthenticationToken(user, null, user.authorities)
  }

  @AfterEach
  fun clearAuthentication() {
    SecurityContextHolder.clearContext()
  }

  private fun createQuestionnaire(type: QuestionnaireType) =
      questionnaireService.create(ROOM_ID, "Binary ${UUID.randomUUID()}", type)

  /** A survey questionnaire keeps the round open, so answers can be given right away. */
  private fun createBinaryContent(correctAnswer: Boolean? = true): Content =
      contentService.create(
          createQuestionnaire(QuestionnaireType.SURVEY).id!!,
          BODY,
          BinaryContentFormatData(correctAnswer),
          null,
          true,
          null,
      )

  /** Each answer comes from a fresh account, since a user may answer a round only once. */
  private fun answer(content: Content, answer: Boolean): Answer {
    authenticate(userService.createAccount().id!!)
    try {
      return answerService.createAnswer(content.activeRound!!.id!!, BinaryAnswerFormatData(answer))
    } finally {
      authenticate(OWNER_ID)
    }
  }

  private fun update(content: Content, correctAnswer: Boolean?) =
      contentService.update(
          content.id!!, BODY, BinaryContentFormatData(correctAnswer), null, true, null)

  private fun reload(content: Content): Content =
      contentService.findById(content.id!!).orElseThrow()

  private fun formatDataOf(content: Content) = reload(content).formatData as BinaryContentFormatData

  @Test
  fun shouldPersistAndReloadBinaryFormat() {
    val content = createBinaryContent()
    val formatData = formatDataOf(content)
    Assertions.assertEquals("BINARY", formatData.format)
    Assertions.assertTrue(formatData.answerable())
    Assertions.assertTrue(formatData.hasCorrectAnswer())
    Assertions.assertEquals(true, formatData.correctAnswer)
    Assertions.assertEquals(Content.BASE_POINTS, reload(content).points)
  }

  @Test
  fun shouldPersistAndReloadBinaryFormatWithoutCorrectAnswer() {
    val formatData = formatDataOf(createBinaryContent(null))
    Assertions.assertFalse(formatData.hasCorrectAnswer())
    Assertions.assertNull(formatData.correctAnswer)
  }

  @Test
  fun shouldPersistAndReloadBinaryAnswerFormat() {
    val answer = answer(createBinaryContent(), false)
    val reloaded = answerRepository.findById(answer.id!!).orElseThrow()
    Assertions.assertEquals("BINARY", reloaded.formatData!!.format)
    Assertions.assertEquals(false, (reloaded.formatData as BinaryAnswerFormatData).answer)
  }

  @Test
  fun shouldScoreMatchingAnswerAsCorrect() {
    val answer = answer(createBinaryContent(false), false)
    Assertions.assertEquals(AnswerResultState.CORRECT, answer.result)
    Assertions.assertEquals(Content.BASE_POINTS, answer.points)
  }

  @Test
  fun shouldScoreDifferingAnswerAsWrongWithoutPoints() {
    val answer = answer(createBinaryContent(false), true)
    Assertions.assertEquals(AnswerResultState.WRONG, answer.result)
    Assertions.assertNull(answer.points)
  }

  @Test
  fun shouldScoreNeutrallyWithoutCorrectAnswer() {
    val answer = answer(createBinaryContent(null), true)
    Assertions.assertEquals(AnswerResultState.NEUTRAL, answer.result)
    Assertions.assertNull(answer.points)
  }

  @Test
  fun shouldCreateAbstention() {
    val content = createBinaryContent()
    val answer = answerService.createAbstention(content.activeRound!!.id!!)
    Assertions.assertEquals(AnswerResultState.ABSTAINED, answer.result)
    Assertions.assertNull(answer.formatData)
  }

  @Test
  fun shouldUpdateCorrectAnswerWithoutAnswers() {
    val content = createBinaryContent()
    update(content, false)
    Assertions.assertEquals(false, formatDataOf(content).correctAnswer)
  }

  @Test
  fun shouldRejectCorrectAnswerChangeOnAnsweredContent() {
    val content = createBinaryContent()
    answer(content, true)
    Assertions.assertThrows(ContentFormatDataInvalidException::class.java) {
      update(content, false)
    }
    Assertions.assertEquals(true, formatDataOf(content).correctAnswer)
  }

  @Test
  fun shouldRejectCorrectAnswerRemovalOnAnsweredContent() {
    val content = createBinaryContent()
    answer(content, true)
    Assertions.assertThrows(ContentFormatDataInvalidException::class.java) { update(content, null) }
    Assertions.assertEquals(true, formatDataOf(content).correctAnswer)
  }

  @Test
  fun shouldUpdateUnrelatedFieldsOnAnsweredContent() {
    val content = createBinaryContent()
    answer(content, true)
    contentService.update(
        content.id!!, OTHER_BODY, BinaryContentFormatData(true), DURATION, false, null)
    val reloaded = reload(content)
    Assertions.assertEquals(OTHER_BODY, reloaded.body)
    Assertions.assertEquals(DURATION, reloaded.duration)
    Assertions.assertFalse(reloaded.abstentionsAllowed)
    Assertions.assertEquals(true, formatDataOf(content).correctAnswer)
  }

  @Test
  fun shouldRejectBinaryUpdateForOtherFormat() {
    val content = createTextContent()
    Assertions.assertThrows(ContentFormatDataInvalidException::class.java) { update(content, true) }
  }

  @Test
  fun shouldRejectBinaryAnswerForOtherFormat() {
    val content = createTextContent()
    Assertions.assertThrows(ContentFormatDataInvalidException::class.java) { answer(content, true) }
  }

  private fun createTextContent(): Content =
      contentService.create(
          createQuestionnaire(QuestionnaireType.SURVEY).id!!,
          BODY,
          TextContentFormatData(false),
          null,
          true,
          null,
      )
}
