/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire

import graphql.ExecutionResult
import java.time.Duration
import java.util.UUID
import java.util.concurrent.TimeUnit
import net.particify.arsnova.core4.TestcontainersConfiguration
import net.particify.arsnova.core4.questionnaire.answer.AnswerServiceImpl
import net.particify.arsnova.core4.questionnaire.answer.ChoiceAnswerFormatData
import net.particify.arsnova.core4.questionnaire.content.ChoiceContentFormatData
import net.particify.arsnova.core4.questionnaire.content.Content
import net.particify.arsnova.core4.questionnaire.content.ContentServiceImpl
import net.particify.arsnova.core4.questionnaire.questionnaire.Questionnaire
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnairePublishingServiceImpl
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnaireServiceImpl
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnaireType
import net.particify.arsnova.core4.user.UserService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.reactivestreams.Publisher
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.graphql.ExecutionGraphQlService
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.support.TransactionTemplate
import reactor.core.publisher.Flux

/** Account and room from the `dev` Liquibase fixtures; the account owns the room. */
private val ROOM_ID = UUID.fromString("3f9cecb5-6dbf-45cc-b630-4af700d474b6")
private val OWNER_ID = UUID.fromString("9c778494-9b52-424d-8c7f-f4936629facb")

/** Longer than a sample interval, so that a recomputation of a missed change would show. */
private val DELIVERY_TIMEOUT = Duration.ofSeconds(10)

/** Time an answer's transaction stays open after its change is announced, if announced early. */
private const val COMMIT_DELAY_MS = 500L

/**
 * Delivers live updates through the real schema, controllers and transactions to the
 * questionnaire's owner, who sees the counts of contents participants cannot see yet.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration::class)
class SubscriptionDeliveryTests {
  @Autowired lateinit var answerService: AnswerServiceImpl
  @Autowired lateinit var contentService: ContentServiceImpl
  @Autowired lateinit var graphQlService: ExecutionGraphQlService
  @Autowired lateinit var publishingService: QuestionnairePublishingServiceImpl
  @Autowired lateinit var questionnaireService: QuestionnaireServiceImpl
  @Autowired lateinit var transactionTemplate: TransactionTemplate
  @Autowired lateinit var userService: UserService

  @BeforeEach
  fun authenticateOwner() {
    val user = userService.loadUserById(OWNER_ID)!!
    SecurityContextHolder.getContext().authentication =
        UsernamePasswordAuthenticationToken(user, null, user.authorities)
  }

  @AfterEach
  fun clearAuthentication() {
    SecurityContextHolder.clearContext()
  }

  /** A live quiz presenting its content, neither published nor with published statistics. */
  private fun presentedContent(): Content {
    val questionnaire = createQuiz()
    val content =
        contentService.create(
            questionnaire.id!!,
            "Choice content",
            ChoiceContentFormatData(listOf("A", "B"), listOf(0), false),
            null,
            true,
            null)
    publishingService.startLiveContent(content.id!!)
    questionnaireService.update(questionnaire.id!!, null, false, null, false, null, null)
    return contentService.findById(content.id!!).orElseThrow()
  }

  private fun createQuiz(): Questionnaire =
      questionnaireService.create(ROOM_ID, "Delivery ${UUID.randomUUID()}", QuestionnaireType.QUIZ)

  private fun subscribe(document: String): Flux<Map<String, Any?>> {
    val response = graphQlService.executeAsCurrentUser(document)
    Assertions.assertTrue(response.errors.isEmpty(), response.errors.toString())
    return Flux.from(response.getData<Publisher<ExecutionResult>>()!!).map {
      Assertions.assertTrue(it.errors.isEmpty(), it.errors.toString())
      it.getData<Map<String, Any?>>()
    }
  }

  /** Announces the change while the transaction is still open, as a slow commit would. */
  private fun answerWithSlowCommit(content: Content) {
    transactionTemplate.executeWithoutResult {
      answerService.createAnswer(content.activeRound!!.id!!, ChoiceAnswerFormatData(listOf(0)))
      Thread.sleep(COMMIT_DELAY_MS)
    }
  }

  @Test
  fun shouldDeliverTheCommittedAnswerCountToTheOwner() {
    val content = presentedContent()
    val updates =
        subscribe(
                """
                subscription {
                  questionnaireContentUpdated(questionnaireId: "${content.questionnaire!!.id}") {
                    id
                    answerCount
                  }
                }
                """
                    .trimIndent())
            .map { it.getValue("questionnaireContentUpdated") as Map<*, *> }
            .filter { it["answerCount"] == 1 }
            .next()
            .toFuture()

    answerWithSlowCommit(content)

    val update = updates.get(DELIVERY_TIMEOUT.toSeconds(), TimeUnit.SECONDS)!!
    Assertions.assertEquals(content.id.toString(), update["id"])
  }

  @Test
  fun shouldDeliverUnpublishedRoundStatsToTheOwner() {
    val content = presentedContent()
    val updates =
        subscribe(
                """
                subscription {
                  questionnaireChoiceRoundStatsUpdated(roundId: "${content.activeRound!!.id}") {
                    selectionCounts
                  }
                }
                """
                    .trimIndent())
            .map { it.getValue("questionnaireChoiceRoundStatsUpdated") as Map<*, *> }
            .filter { it["selectionCounts"] == listOf(1, 0) }
            .next()
            .toFuture()

    answerWithSlowCommit(content)

    Assertions.assertNotNull(updates.get(DELIVERY_TIMEOUT.toSeconds(), TimeUnit.SECONDS))
  }
}
