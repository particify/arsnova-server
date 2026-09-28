/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import net.particify.arsnova.core4.TestcontainersConfiguration
import net.particify.arsnova.core4.questionnaire.answer.AnswerRepository
import net.particify.arsnova.core4.questionnaire.answer.AnswerResultState
import net.particify.arsnova.core4.questionnaire.answer.AnswerServiceImpl
import net.particify.arsnova.core4.questionnaire.answer.ScaleAnswerFormatData
import net.particify.arsnova.core4.questionnaire.content.Content
import net.particify.arsnova.core4.questionnaire.content.ContentServiceImpl
import net.particify.arsnova.core4.questionnaire.content.ScaleContentFormatData
import net.particify.arsnova.core4.questionnaire.content.TextContentFormatData
import net.particify.arsnova.core4.questionnaire.exception.ContentFormatDataInvalidException
import net.particify.arsnova.core4.questionnaire.exception.RoundClosedException
import net.particify.arsnova.core4.questionnaire.exception.RoundExpiredException
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnairePublishingServiceImpl
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnaireServiceImpl
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnaireType
import net.particify.arsnova.core4.questionnaire.round.Round
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
private val OWNER_ID = UUID.fromString("9c778494-9b52-424d-8c7f-f4936629facb")
private const val DURATION = 30
private const val BODY = "How much do you agree?"
private const val OTHER_BODY = "How much do you really agree?"
private val TEMPLATE = ScaleContentFormatData.Template.AGREEMENT
private val OTHER_TEMPLATE = ScaleContentFormatData.Template.QUALITY

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration::class)
class ScaleContentTests {
  @Autowired lateinit var answerRepository: AnswerRepository
  @Autowired lateinit var answerService: AnswerServiceImpl
  @Autowired lateinit var contentService: ContentServiceImpl
  @Autowired lateinit var publishingService: QuestionnairePublishingServiceImpl
  @Autowired lateinit var questionnaireService: QuestionnaireServiceImpl
  @Autowired lateinit var roundService: RoundServiceImpl
  @Autowired lateinit var userService: UserService

  @BeforeEach
  fun authenticateUser() {
    val user = userService.loadUserById(OWNER_ID)!!
    SecurityContextHolder.getContext().authentication =
        UsernamePasswordAuthenticationToken(user, null, user.authorities)
  }

  @AfterEach
  fun clearAuthentication() {
    SecurityContextHolder.clearContext()
  }

  private fun createQuestionnaire(type: QuestionnaireType) =
      questionnaireService.create(ROOM_ID, "Scale ${UUID.randomUUID()}", type)

  private fun createScaleContent(
      questionnaireId: UUID,
      neutralOption: Boolean = true,
      duration: Int? = null
  ): Content =
      contentService.create(
          questionnaireId,
          BODY,
          ScaleContentFormatData(TEMPLATE, neutralOption),
          duration,
          true,
          null,
      )

  /** A survey leaves the round open, unlike a quiz. */
  private fun createAnswerableContent(neutralOption: Boolean = true): Content =
      createScaleContent(createQuestionnaire(QuestionnaireType.SURVEY).id!!, neutralOption)

  private fun answer(content: Content, rating: Int) =
      answerService.createAnswer(content.activeRound!!.id!!, ScaleAnswerFormatData(rating))

  private fun reload(content: Content): Content =
      contentService.findById(content.id!!).orElseThrow()

  private fun formatDataOf(content: Content) = reload(content).formatData as ScaleContentFormatData

  private fun expire(round: Round): Round {
    round.answeringEndTime = Instant.now().minus(1, ChronoUnit.MINUTES)
    return roundService.save(round)
  }

  @Test
  fun shouldPersistAndReloadScaleFormatWithNeutralOption() {
    val content = createAnswerableContent()
    val formatData = formatDataOf(content)
    Assertions.assertEquals("SCALE", formatData.format)
    Assertions.assertTrue(formatData.answerable())
    Assertions.assertFalse(formatData.hasCorrectAnswer())
    Assertions.assertEquals(TEMPLATE, formatData.template)
    Assertions.assertTrue(formatData.neutralOption)
    Assertions.assertEquals(5, formatData.optionCount())
    Assertions.assertEquals(0, reload(content).points)
  }

  @Test
  fun shouldPersistAndReloadScaleFormatWithoutNeutralOption() {
    val formatData = formatDataOf(createAnswerableContent(neutralOption = false))
    Assertions.assertFalse(formatData.neutralOption)
    Assertions.assertEquals(4, formatData.optionCount())
  }

  @Test
  fun shouldPersistAndReloadScaleAnswerFormat() {
    val answer = answer(createAnswerableContent(), 2)
    val reloaded = answerRepository.findById(answer.id!!).orElseThrow()
    Assertions.assertEquals("SCALE", reloaded.formatData!!.format)
    Assertions.assertEquals(2, (reloaded.formatData as ScaleAnswerFormatData).rating)
  }

  @Test
  fun shouldCreateNeutralAnswerWithoutPoints() {
    val answer = answer(createAnswerableContent(), 0)
    Assertions.assertEquals(AnswerResultState.NEUTRAL, answer.result)
    Assertions.assertNull(answer.points)
  }

  @Test
  fun shouldAcceptRatingAtBoundsWithNeutralOption() {
    val content = createAnswerableContent()
    Assertions.assertEquals(AnswerResultState.NEUTRAL, answer(content, 0).result)
    Assertions.assertEquals(AnswerResultState.NEUTRAL, answer(content, 4).result)
  }

  @Test
  fun shouldAcceptRatingAtBoundsWithoutNeutralOption() {
    val content = createAnswerableContent(neutralOption = false)
    Assertions.assertEquals(AnswerResultState.NEUTRAL, answer(content, 0).result)
    Assertions.assertEquals(AnswerResultState.NEUTRAL, answer(content, 3).result)
  }

  @Test
  fun shouldRejectRatingBelowLowerBound() {
    val content = createAnswerableContent()
    Assertions.assertThrows(ContentFormatDataInvalidException::class.java) { answer(content, -1) }
  }

  @Test
  fun shouldRejectRatingAboveUpperBoundWithNeutralOption() {
    val content = createAnswerableContent()
    Assertions.assertThrows(ContentFormatDataInvalidException::class.java) { answer(content, 5) }
  }

  @Test
  fun shouldRejectRatingAboveUpperBoundWithoutNeutralOption() {
    val content = createAnswerableContent(neutralOption = false)
    Assertions.assertThrows(ContentFormatDataInvalidException::class.java) { answer(content, 4) }
  }

  @Test
  fun shouldCreateAbstention() {
    val answer = answerService.createAbstention(createAnswerableContent().activeRound!!.id!!)
    Assertions.assertEquals(AnswerResultState.ABSTAINED, answer.result)
    Assertions.assertNull(answer.formatData)
  }

  @Test
  fun shouldRejectScaleAnswerOnClosedRound() {
    val content = createScaleContent(createQuestionnaire(QuestionnaireType.QUIZ).id!!)
    Assertions.assertThrows(RoundClosedException::class.java) { answer(content, 0) }
  }

  @Test
  fun shouldRejectScaleAnswerAfterDeadline() {
    val content =
        createScaleContent(createQuestionnaire(QuestionnaireType.QUIZ).id!!, duration = DURATION)
    val round = expire(publishingService.startLiveContent(content.id!!).activeRound!!)
    Assertions.assertThrows(RoundExpiredException::class.java) {
      answerService.createAnswer(round.id!!, ScaleAnswerFormatData(0))
    }
  }

  @Test
  fun shouldUpdateTemplateAndNeutralOptionWithoutAnswers() {
    val content = createAnswerableContent()
    contentService.update(
        content.id!!, BODY, ScaleContentFormatData(OTHER_TEMPLATE, false), null, true, null)
    val formatData = formatDataOf(content)
    Assertions.assertEquals(OTHER_TEMPLATE, formatData.template)
    Assertions.assertFalse(formatData.neutralOption)
  }

  @Test
  fun shouldRejectTemplateChangeOnAnsweredContent() {
    val content = createAnswerableContent()
    answer(content, 0)
    Assertions.assertThrows(ContentFormatDataInvalidException::class.java) {
      contentService.update(
          content.id!!, BODY, ScaleContentFormatData(OTHER_TEMPLATE, true), null, true, null)
    }
    Assertions.assertEquals(TEMPLATE, formatDataOf(content).template)
  }

  @Test
  fun shouldRejectNeutralOptionChangeOnAnsweredContent() {
    val content = createAnswerableContent()
    answer(content, 0)
    Assertions.assertThrows(ContentFormatDataInvalidException::class.java) {
      contentService.update(
          content.id!!, BODY, ScaleContentFormatData(TEMPLATE, false), null, true, null)
    }
    Assertions.assertTrue(formatDataOf(content).neutralOption)
  }

  @Test
  fun shouldUpdateUnrelatedFieldsOnAnsweredContent() {
    val content = createAnswerableContent()
    answer(content, 0)
    contentService.update(
        content.id!!, OTHER_BODY, ScaleContentFormatData(TEMPLATE, true), DURATION, false, null)
    val reloaded = reload(content)
    Assertions.assertEquals(OTHER_BODY, reloaded.body)
    Assertions.assertEquals(DURATION, reloaded.duration)
    Assertions.assertFalse(reloaded.abstentionsAllowed)
  }

  @Test
  fun shouldRejectTemplateChangeForAnswerInPreviousRound() {
    val content = createAnswerableContent()
    answer(content, 0)
    roundService.startNewRound(content.id!!)
    Assertions.assertThrows(ContentFormatDataInvalidException::class.java) {
      contentService.update(
          content.id!!, BODY, ScaleContentFormatData(OTHER_TEMPLATE, true), null, true, null)
    }
  }

  @Test
  fun shouldRejectScaleUpdateForNonScaleContent() {
    val content =
        contentService.create(
            createQuestionnaire(QuestionnaireType.SURVEY).id!!,
            BODY,
            TextContentFormatData(false),
            null,
            true,
            null,
        )
    Assertions.assertThrows(ContentFormatDataInvalidException::class.java) {
      contentService.update(
          content.id!!, BODY, ScaleContentFormatData(TEMPLATE, true), null, true, null)
    }
  }
}
