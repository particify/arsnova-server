/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.round.api

import java.time.Duration
import java.util.UUID
import net.particify.arsnova.core4.questionnaire.round.ChoiceContentRoundStats
import net.particify.arsnova.core4.questionnaire.round.RoundStatsServiceImpl
import net.particify.arsnova.core4.questionnaire.round.WordcloudContentRoundStats
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import reactor.test.StepVerifier

private val SUBSCRIBED_ROUND_ID = UUID.randomUUID()
private val OTHER_ROUND_ID = UUID.randomUUID()
private val CONTENT_ID = UUID.randomUUID()
private val QUESTIONNAIRE_ID = UUID.randomUUID()

// Must match STATS_UPDATE_SAMPLE_INTERVAL of the controller under test.
private val SAMPLE_INTERVAL = Duration.ofSeconds(3)
private val VERIFY_TIMEOUT = Duration.ofSeconds(10)

class RoundStatsSubscriptionControllerTest {
  private val eventPublisher = RoundStatsEventPublisher()
  private val roundStatsServiceImpl = Mockito.mock(RoundStatsServiceImpl::class.java)
  private val controller = RoundStatsSubscriptionController(eventPublisher, roundStatsServiceImpl)

  @Test
  fun shouldEmitChoiceStatsForAnswerOfSubscribedRound() {
    val stats = ChoiceContentRoundStats(listOf(2, 1))
    Mockito.`when`(roundStatsServiceImpl.getChoiceRoundStatsByRoundId(SUBSCRIBED_ROUND_ID))
        .thenReturn(stats)

    StepVerifier.withVirtualTime {
          controller.questionnaireChoiceRoundStatsUpdated(SUBSCRIBED_ROUND_ID)
        }
        .expectSubscription()
        .then { publishAnswerChanged(SUBSCRIBED_ROUND_ID) }
        .thenAwait(SAMPLE_INTERVAL)
        .expectNext(stats)
        .thenCancel()
        .verify(VERIFY_TIMEOUT)
  }

  @Test
  fun shouldNotEmitChoiceStatsForAnswerOfOtherRound() {
    StepVerifier.withVirtualTime {
          controller.questionnaireChoiceRoundStatsUpdated(SUBSCRIBED_ROUND_ID)
        }
        .expectSubscription()
        .then { publishAnswerChanged(OTHER_ROUND_ID) }
        .expectNoEvent(SAMPLE_INTERVAL.multipliedBy(2))
        .thenCancel()
        .verify(VERIFY_TIMEOUT)
  }

  @Test
  fun shouldEmitWordcloudStatsForAnswerOfSubscribedRound() {
    val stats = WordcloudContentRoundStats(listOf("word"), listOf(1))
    Mockito.`when`(roundStatsServiceImpl.getWordcloudRoundStatsByRoundId(SUBSCRIBED_ROUND_ID))
        .thenReturn(stats)

    StepVerifier.withVirtualTime {
          controller.questionnaireWordcloudRoundStatsUpdated(SUBSCRIBED_ROUND_ID)
        }
        .expectSubscription()
        .then { publishAnswerChanged(SUBSCRIBED_ROUND_ID) }
        .thenAwait(SAMPLE_INTERVAL)
        .expectNext(stats)
        .thenCancel()
        .verify(VERIFY_TIMEOUT)
  }

  @Test
  fun shouldNotEmitWordcloudStatsForAnswerOfOtherRound() {
    StepVerifier.withVirtualTime {
          controller.questionnaireWordcloudRoundStatsUpdated(SUBSCRIBED_ROUND_ID)
        }
        .expectSubscription()
        .then { publishAnswerChanged(OTHER_ROUND_ID) }
        .expectNoEvent(SAMPLE_INTERVAL.multipliedBy(2))
        .thenCancel()
        .verify(VERIFY_TIMEOUT)
  }

  private fun publishAnswerChanged(roundId: UUID) {
    eventPublisher.publishAnswerChanged(
        AnswerChangedEvent(
            roundId = roundId, contentId = CONTENT_ID, questionnaireId = QUESTIONNAIRE_ID))
  }
}
