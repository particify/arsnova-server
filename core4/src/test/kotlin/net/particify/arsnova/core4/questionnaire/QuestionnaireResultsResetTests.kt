/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire

import graphql.ErrorClassification
import graphql.ExecutionResult
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.BlockingQueue
import java.util.concurrent.ExecutionException
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import net.particify.arsnova.core4.TestcontainersConfiguration
import net.particify.arsnova.core4.questionnaire.answer.AnswerServiceImpl
import net.particify.arsnova.core4.questionnaire.answer.ChoiceAnswerFormatData
import net.particify.arsnova.core4.questionnaire.content.ChoiceContentFormatData
import net.particify.arsnova.core4.questionnaire.content.Content
import net.particify.arsnova.core4.questionnaire.content.ContentServiceImpl
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnairePublishingMode
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
import org.springframework.graphql.execution.ErrorType
import org.springframework.graphql.execution.SubscriptionPublisherException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.ActiveProfiles
import reactor.core.Disposable
import reactor.core.publisher.Flux
import reactor.core.publisher.Signal

/** Account and room from the `dev` Liquibase fixtures; the account owns the room. */
private val ROOM_ID = UUID.fromString("3f9cecb5-6dbf-45cc-b630-4af700d474b6")
private val OWNER_ID = UUID.fromString("9c778494-9b52-424d-8c7f-f4936629facb")

/** Longer than a sample interval, so that a recomputation following the reset would show. */
private val DELIVERY_TIMEOUT = Duration.ofSeconds(10)

private val STATS_BEFORE_RESET = listOf(1, 0)

/**
 * Resetting the results replaces every round of the questionnaire by a fresh one with a new id,
 * while a presentation is subscribed to the old rounds and to the questionnaire's contents.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration::class)
class QuestionnaireResultsResetTests {
  @Autowired lateinit var answerService: AnswerServiceImpl
  @Autowired lateinit var contentService: ContentServiceImpl
  @Autowired lateinit var graphQlService: ExecutionGraphQlService
  @Autowired lateinit var questionnaireService: QuestionnaireServiceImpl
  @Autowired lateinit var userService: UserService

  private val subscriptions = mutableListOf<Disposable>()

  @BeforeEach
  fun authenticateOwner() {
    val user = userService.loadUserById(OWNER_ID)!!
    SecurityContextHolder.getContext().authentication =
        UsernamePasswordAuthenticationToken(user, null, user.authorities)
  }

  @AfterEach
  fun disposeSubscriptions() {
    subscriptions.forEach { it.dispose() }
    SecurityContextHolder.clearContext()
  }

  /** A content of a quiz which releases its contents up to a chosen one, as the first one. */
  private fun releasedContent(): Content {
    val questionnaire =
        questionnaireService.create(ROOM_ID, "Reset ${UUID.randomUUID()}", QuestionnaireType.QUIZ)
    questionnaireService.update(
        questionnaire.id!!, null, true, QuestionnairePublishingMode.UP_TO, null, null, null)
    val content =
        contentService.create(
            questionnaire.id!!,
            "Choice content",
            ChoiceContentFormatData(listOf("A", "B"), listOf(0), false),
            null,
            true,
            null)
    return contentService.findById(content.id!!).orElseThrow()
  }

  private fun answer(content: Content) {
    answerService.createAnswer(content.activeRound!!.id!!, ChoiceAnswerFormatData(listOf(0)))
  }

  private fun currentRoundId(content: Content): UUID =
      contentService.findById(content.id!!).orElseThrow().activeRound!!.id!!

  /** Every signal of the subscription, so that an error or completion can be awaited too. */
  private fun subscribe(document: String, field: String): BlockingQueue<Signal<Map<*, *>>> {
    val response = graphQlService.executeAsCurrentUser(document)
    Assertions.assertTrue(response.errors.isEmpty(), response.errors.toString())
    val signals = LinkedBlockingQueue<Signal<Map<*, *>>>()
    subscriptions.add(
        Flux.from(response.getData<Publisher<ExecutionResult>>()!!)
            .map {
              Assertions.assertTrue(it.errors.isEmpty(), it.errors.toString())
              it.getData<Map<String, Any?>>().getValue(field) as Map<*, *>
            }
            .materialize()
            .subscribe { signals.add(it) })
    return signals
  }

  /** The next signal not matching [skipped], or null if none arrives in time. */
  private fun <T : Any> nextSignal(
      signals: BlockingQueue<Signal<T>>,
      skipped: (Signal<T>) -> Boolean = { false }
  ): Signal<T>? {
    val deadline = Instant.now().plus(DELIVERY_TIMEOUT)
    var signal: Signal<T>?
    do {
      val left = Duration.between(Instant.now(), deadline)
      signal = if (left.isNegative) null else signals.poll(left.toMillis(), TimeUnit.MILLISECONDS)
    } while (signal != null && skipped(signal))
    return signal
  }

  private fun subscribeToChoiceStats(roundId: UUID) =
      subscribe(
          """
          subscription {
            questionnaireChoiceRoundStatsUpdated(roundId: "$roundId") {
              selectionCounts
            }
          }
          """
              .trimIndent(),
          "questionnaireChoiceRoundStatsUpdated")

  private fun subscribeToContents(questionnaireId: UUID) =
      subscribe(
          """
          subscription {
            questionnaireContentUpdated(questionnaireId: "$questionnaireId") {
              id
              activeRound {
                id
              }
            }
          }
          """
              .trimIndent(),
          "questionnaireContentUpdated")

  /**
   * The round is deleted by the reset, and nothing is ever signalled for it again, so a
   * subscription to it which is open at that moment keeps the results from before the reset on
   * screen. It has to learn of the reset: by ending, failing, or emitting results without the
   * deleted answers.
   */
  @Test
  fun shouldNotLeaveAnOpenStatsSubscriptionShowingTheResultsFromBeforeTheReset() {
    val content = releasedContent()
    answer(content)
    val stats = subscribeToChoiceStats(content.activeRound!!.id!!)
    val initial = nextSignal(stats)
    Assertions.assertEquals(STATS_BEFORE_RESET, initial?.get()?.get("selectionCounts"))

    questionnaireService.resetAnswerResultsByQuestionnaireId(content.questionnaire!!.id!!)

    val afterReset =
        nextSignal(stats) { it.isOnNext && it.get()?.get("selectionCounts") == STATS_BEFORE_RESET }
    Assertions.assertNotNull(
        afterReset,
        "The subscription to the deleted round neither ended nor emitted anything after the reset")
  }

  /**
   * A presentation opened after the last answer joins a content's shared stream on the reset's own
   * signal, while another subscriber keeps that stream alive, so it is handed the content read
   * before the reset. Its round still has to be the fresh one, or subscribing to the round's stats
   * fails with a round that no longer exists.
   */
  @Test
  fun shouldHandAPresentationJoiningOnTheResetTheFreshRound() {
    val content = releasedContent()
    val questionnaireId = content.questionnaire!!.id!!
    val earlier = subscribeToContents(questionnaireId)
    answer(content)
    Assertions.assertEquals(
        content.id.toString(), nextSignal(earlier)?.get()?.get("id"), "The stream did not start")
    val presentation = subscribeToContents(questionnaireId)

    questionnaireService.resetAnswerResultsByQuestionnaireId(questionnaireId)

    val first = nextSignal(presentation)
    Assertions.assertTrue(first?.isOnNext == true, "The presentation was not told of the reset")
    Assertions.assertEquals(
        currentRoundId(content).toString(), (first!!.get()!!["activeRound"] as Map<*, *>)["id"])
  }

  @Test
  fun shouldEndAnOpenStatsSubscriptionWithNotFoundOnTheReset() {
    val content = releasedContent()
    answer(content)

    val errors =
        statsSubscriptionErrors(content) {
          questionnaireService.resetAnswerResultsByQuestionnaireId(content.questionnaire!!.id!!)
        }

    Assertions.assertEquals(listOf(ErrorType.NOT_FOUND), errors)
  }

  @Test
  fun shouldRejectASubscriptionToADeletedRoundAsNotFound() {
    val content = releasedContent()
    questionnaireService.resetAnswerResultsByQuestionnaireId(content.questionnaire!!.id!!)

    Assertions.assertEquals(listOf(ErrorType.NOT_FOUND), statsSubscriptionErrors(content) {})
  }

  /**
   * The classifications of the errors a stats subscription to the round of [content] fails with,
   * whether on being set up or, after [action], while running.
   */
  private fun statsSubscriptionErrors(
      content: Content,
      action: () -> Unit
  ): List<ErrorClassification?> {
    val response = graphQlService.executeAsCurrentUser(choiceStatsSubscription(content))
    if (response.errors.isNotEmpty()) return response.errors.map { it.errorType }
    val ended = Flux.from(response.getData<Publisher<ExecutionResult>>()!!).then().toFuture()
    action()
    val failure =
        Assertions.assertThrows(ExecutionException::class.java) {
          ended.get(DELIVERY_TIMEOUT.toSeconds(), TimeUnit.SECONDS)
        }
    return (failure.cause as SubscriptionPublisherException).errors.map { it.errorType }
  }

  @Test
  fun shouldReportAQueryOnADeletedRoundAsNotFound() {
    val content = releasedContent()
    questionnaireService.resetAnswerResultsByQuestionnaireId(content.questionnaire!!.id!!)

    val response =
        graphQlService.executeAsCurrentUser(
            """
            query {
              questionnaireChoiceRoundStatsByRoundId(roundId: "${content.activeRound!!.id}") {
                selectionCounts
              }
            }
            """
                .trimIndent())

    Assertions.assertEquals(listOf(ErrorType.NOT_FOUND), response.errors.map { it.errorType })
  }

  private fun choiceStatsSubscription(content: Content) =
      """
      subscription {
        questionnaireChoiceRoundStatsUpdated(roundId: "${content.activeRound!!.id}") {
          selectionCounts
        }
      }
      """
          .trimIndent()
}
