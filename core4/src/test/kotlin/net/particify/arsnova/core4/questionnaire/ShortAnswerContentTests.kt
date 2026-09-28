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
import net.particify.arsnova.core4.questionnaire.answer.ShortAnswerAnswerFormatData
import net.particify.arsnova.core4.questionnaire.content.Content
import net.particify.arsnova.core4.questionnaire.content.ContentServiceImpl
import net.particify.arsnova.core4.questionnaire.content.ShortAnswerContentFormatData
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
import org.springframework.graphql.ExecutionGraphQlService
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.ActiveProfiles

private val ROOM_ID = UUID.fromString("3f9cecb5-6dbf-45cc-b630-4af700d474b6")
private val OWNER_ID = UUID.fromString("9c778494-9b52-424d-8c7f-f4936629facb")
private const val DURATION = 30
private const val BODY = "Which city hosts the ARS conference?"
private const val OTHER_BODY = "Which city hosted the ARS conference?"
private val TERMS = listOf("Frankfurt am Main", "Frankfurt")
private val OTHER_TERMS = listOf("Offenbach")

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration::class)
class ShortAnswerContentTests {
  @Autowired lateinit var answerRepository: AnswerRepository
  @Autowired lateinit var answerService: AnswerServiceImpl
  @Autowired lateinit var contentService: ContentServiceImpl
  @Autowired lateinit var graphQlService: ExecutionGraphQlService
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
      questionnaireService.create(ROOM_ID, "Short answer ${UUID.randomUUID()}", type)

  private fun createShortAnswerContent(correctTerms: List<String> = TERMS): Content =
      contentService.create(
          createQuestionnaire(QuestionnaireType.SURVEY).id!!,
          BODY,
          ShortAnswerContentFormatData(correctTerms),
          null,
          true,
          null,
      )

  /** Each answer comes from a fresh account, since a user may answer a round only once. */
  private fun answer(content: Content, body: String): Answer {
    authenticate(userService.createAccount().id!!)
    try {
      return answerService.createAnswer(
          content.activeRound!!.id!!, ShortAnswerAnswerFormatData(body))
    } finally {
      authenticate(OWNER_ID)
    }
  }

  private fun reload(content: Content): Content =
      contentService.findById(content.id!!).orElseThrow()

  private fun formatDataOf(content: Content) =
      reload(content).formatData as ShortAnswerContentFormatData

  @Test
  fun shouldPersistAndReloadShortAnswerFormat() {
    val content = createShortAnswerContent()
    val formatData = formatDataOf(content)
    Assertions.assertEquals("SHORT_ANSWER", formatData.format)
    Assertions.assertTrue(formatData.answerable())
    Assertions.assertTrue(formatData.hasCorrectAnswer())
    Assertions.assertEquals(TERMS, formatData.correctTerms)
    Assertions.assertEquals(Content.BASE_POINTS, reload(content).points)
  }

  @Test
  fun shouldPersistAndReloadShortAnswerAnswerFormat() {
    val answer = answer(createShortAnswerContent(), "Frankfurt")
    val reloaded = answerRepository.findById(answer.id!!).orElseThrow()
    Assertions.assertEquals("SHORT_ANSWER", reloaded.formatData!!.format)
    Assertions.assertEquals("Frankfurt", (reloaded.formatData as ShortAnswerAnswerFormatData).body)
  }

  @Test
  fun shouldScoreExactMatchAsCorrect() {
    val answer = answer(createShortAnswerContent(), "Frankfurt am Main")
    Assertions.assertEquals(AnswerResultState.CORRECT, answer.result)
    Assertions.assertEquals(Content.BASE_POINTS, answer.points)
  }

  @Test
  fun shouldIgnoreCaseAndPunctuationWhenScoring() {
    val content = createShortAnswerContent()
    Assertions.assertEquals(AnswerResultState.CORRECT, answer(content, "FRANKFURT").result)
    Assertions.assertEquals(AnswerResultState.CORRECT, answer(content, "frankfurt am main!").result)
    Assertions.assertEquals(AnswerResultState.CORRECT, answer(content, "Frankfurt-am-Main").result)
  }

  @Test
  fun shouldScoreAnyOfTheCorrectTermsAsCorrect() {
    val content = createShortAnswerContent(listOf("Bonn", "Cologne"))
    Assertions.assertEquals(AnswerResultState.CORRECT, answer(content, "Bonn").result)
    Assertions.assertEquals(AnswerResultState.CORRECT, answer(content, "Cologne").result)
  }

  @Test
  fun shouldScoreOtherTextAsWrongWithoutPoints() {
    val answer = answer(createShortAnswerContent(), "Offenbach")
    Assertions.assertEquals(AnswerResultState.WRONG, answer.result)
    Assertions.assertNull(answer.points)
  }

  @Test
  fun shouldCreateAbstention() {
    val content = createShortAnswerContent()
    val answer = answerService.createAbstention(content.activeRound!!.id!!)
    Assertions.assertEquals(AnswerResultState.ABSTAINED, answer.result)
    Assertions.assertNull(answer.formatData)
  }

  @Test
  fun shouldRejectContentWithoutCorrectTerms() {
    Assertions.assertThrows(ContentFormatDataInvalidException::class.java) {
      createShortAnswerContent(emptyList())
    }
  }

  @Test
  fun shouldRejectContentWithTooManyCorrectTerms() {
    val tooMany = (0..ShortAnswerContentFormatData.MAX_TERM_COUNT).map { "Term $it" }
    Assertions.assertThrows(ContentFormatDataInvalidException::class.java) {
      createShortAnswerContent(tooMany)
    }
  }

  @Test
  fun shouldRejectBlankCorrectTerm() {
    Assertions.assertThrows(ContentFormatDataInvalidException::class.java) {
      createShortAnswerContent(listOf("Frankfurt", " "))
    }
  }

  @Test
  fun shouldRejectOverLengthCorrectTerm() {
    val tooLong = "a".repeat(ShortAnswerContentFormatData.MAX_TERM_LENGTH + 1)
    Assertions.assertThrows(ContentFormatDataInvalidException::class.java) {
      createShortAnswerContent(listOf(tooLong))
    }
  }

  @Test
  fun shouldRejectCorrectTermsThatDifferOnlyInCaseOrPunctuation() {
    Assertions.assertThrows(ContentFormatDataInvalidException::class.java) {
      createShortAnswerContent(listOf("Frankfurt am Main", "frankfurt-am-main"))
    }
  }

  @Test
  fun shouldUpdateCorrectTermsWithoutAnswers() {
    val content = createShortAnswerContent()
    contentService.update(
        content.id!!, BODY, ShortAnswerContentFormatData(OTHER_TERMS), null, true, null)
    Assertions.assertEquals(OTHER_TERMS, formatDataOf(content).correctTerms)
  }

  @Test
  fun shouldReorderAndRespellCorrectTermsOnAnsweredContent() {
    val content = createShortAnswerContent()
    answer(content, "Frankfurt")
    val reordered = listOf("frankfurt!", "Frankfurt am Main")
    contentService.update(
        content.id!!, BODY, ShortAnswerContentFormatData(reordered), null, true, null)
    Assertions.assertEquals(reordered, formatDataOf(content).correctTerms)
  }

  @Test
  fun shouldRejectCorrectTermChangeOnAnsweredContent() {
    val content = createShortAnswerContent()
    answer(content, "Frankfurt")
    Assertions.assertThrows(ContentFormatDataInvalidException::class.java) {
      contentService.update(
          content.id!!, BODY, ShortAnswerContentFormatData(OTHER_TERMS), null, true, null)
    }
    Assertions.assertEquals(TERMS, formatDataOf(content).correctTerms)
  }

  @Test
  fun shouldUpdateUnrelatedFieldsOnAnsweredContent() {
    val content = createShortAnswerContent()
    answer(content, "Frankfurt")
    contentService.update(
        content.id!!, OTHER_BODY, ShortAnswerContentFormatData(TERMS), DURATION, false, null)
    val reloaded = reload(content)
    Assertions.assertEquals(OTHER_BODY, reloaded.body)
    Assertions.assertEquals(DURATION, reloaded.duration)
    Assertions.assertFalse(reloaded.abstentionsAllowed)
    Assertions.assertEquals(TERMS, formatDataOf(content).correctTerms)
  }

  @Test
  fun shouldRejectShortAnswerUpdateForOtherFormat() {
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
          content.id!!, BODY, ShortAnswerContentFormatData(TERMS), null, true, null)
    }
  }

  @Test
  fun shouldBanAndResetKeywordsThroughGraphQl() {
    val content = createShortAnswerContent()
    val banned =
        graphQlService.executeAsCurrentUser(
            """
            mutation {
              banQuestionnaireShortAnswerContentKeyword(contentId: "${content.id}", keyword: "Spam!") {
                id
              }
            }
            """)
    Assertions.assertTrue(banned.errors.isEmpty()) { banned.errors.toString() }
    Assertions.assertEquals(
        mapOf("banQuestionnaireShortAnswerContentKeyword" to mapOf("id" to content.id.toString())),
        banned.getData<Map<String, Any?>>())
    Assertions.assertEquals(listOf("Spam!"), formatDataOf(content).bannedKeywords)
    val reset =
        graphQlService.executeAsCurrentUser(
            """
            mutation {
              resetBannedQuestionnaireShortAnswerContentKeywords(contentId: "${content.id}") { id }
            }
            """)
    Assertions.assertTrue(reset.errors.isEmpty()) { reset.errors.toString() }
    Assertions.assertEquals(emptyList<String>(), formatDataOf(content).bannedKeywords)
  }

  @Test
  fun shouldKeepBannedKeywordsWhenTheContentIsUpdated() {
    val content = createShortAnswerContent()
    contentService.banShortAnswerKeyword(content.id!!, "Spam")
    contentService.update(
        content.id!!, OTHER_BODY, ShortAnswerContentFormatData(TERMS), null, true, null)
    Assertions.assertEquals(listOf("Spam"), formatDataOf(content).bannedKeywords)
  }

  @Test
  fun shouldRejectKeywordBanThroughGraphQlForOtherFormat() {
    val content =
        contentService.create(
            createQuestionnaire(QuestionnaireType.SURVEY).id!!,
            BODY,
            TextContentFormatData(false),
            null,
            true,
            null,
        )
    val response =
        graphQlService.executeAsCurrentUser(
            """
            mutation {
              banQuestionnaireShortAnswerContentKeyword(contentId: "${content.id}", keyword: "Spam") {
                id
              }
            }
            """)
    Assertions.assertFalse(response.errors.isEmpty())
    Assertions.assertEquals(TextContentFormatData(false), reload(content).formatData)
  }

  @Test
  fun shouldRejectShortAnswerAnswerForOtherFormat() {
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
      answer(content, "Frankfurt")
    }
  }
}
