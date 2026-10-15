/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.content.api

import java.time.Duration
import java.util.Optional
import java.util.UUID
import net.particify.arsnova.core4.questionnaire.content.Content
import net.particify.arsnova.core4.questionnaire.content.ContentRepository
import net.particify.arsnova.core4.questionnaire.questionnaire.Questionnaire
import net.particify.arsnova.core4.questionnaire.round.api.AnswerChangedEvent
import net.particify.arsnova.core4.questionnaire.round.api.RoundStatsEventPublisher
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.security.access.PermissionEvaluator
import org.springframework.security.authentication.TestingAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import reactor.test.StepVerifier

private val QUESTIONNAIRE_ID = UUID.randomUUID()

// Must match STATS_UPDATE_SAMPLE_INTERVAL of the controller under test.
private val SAMPLE_INTERVAL = Duration.ofSeconds(3)
private val VERIFY_TIMEOUT = Duration.ofSeconds(10)

/**
 * Emissions run without the subscriber's security context, so each test clears it before the first
 * answer: the controller has to use the authentication it captured at subscription.
 */
class ContentSubscriptionControllerTest {
  private val eventPublisher = RoundStatsEventPublisher()
  private val contentRepository = Mockito.mock(ContentRepository::class.java)
  private val permissionEvaluator = Mockito.mock(PermissionEvaluator::class.java)
  private val authentication = TestingAuthenticationToken("participant", null, "ROLE_USER")
  private val controller =
      ContentSubscriptionController(eventPublisher, contentRepository, permissionEvaluator)
  private val questionnaire = Questionnaire(id = QUESTIONNAIRE_ID)
  private val releasedContent = content(position = 0)
  private val unreleasedContent = content(position = 1)

  private fun content(position: Int): Content {
    val content =
        Content(id = UUID.randomUUID(), questionnaire = questionnaire, position = position)
    Mockito.`when`(contentRepository.findById(content.id!!)).thenReturn(Optional.of(content))
    return content
  }

  @BeforeEach
  fun authenticate() {
    SecurityContextHolder.getContext().authentication = authentication
    Mockito.`when`(permissionEvaluator.hasPermission(authentication, releasedContent, "read"))
        .thenReturn(true)
    Mockito.`when`(permissionEvaluator.hasPermission(authentication, unreleasedContent, "read"))
        .thenReturn(false)
  }

  @AfterEach
  fun clearAuthentication() {
    SecurityContextHolder.clearContext()
  }

  @Test
  fun shouldEmitReadableContentOnAnswer() {
    StepVerifier.withVirtualTime { controller.questionnaireContentUpdated(QUESTIONNAIRE_ID) }
        .expectSubscription()
        .then {
          SecurityContextHolder.clearContext()
          publishAnswerChanged(releasedContent)
        }
        .thenAwait(SAMPLE_INTERVAL)
        .expectNext(releasedContent)
        .thenCancel()
        .verify(VERIFY_TIMEOUT)
  }

  /** The presenter can answer, or ban a keyword of, a content not yet released to participants. */
  @Test
  fun shouldNotEmitUnreadableContentOnAnswer() {
    StepVerifier.withVirtualTime { controller.questionnaireContentUpdated(QUESTIONNAIRE_ID) }
        .expectSubscription()
        .then {
          SecurityContextHolder.clearContext()
          publishAnswerChanged(unreleasedContent)
        }
        .expectNoEvent(SAMPLE_INTERVAL.multipliedBy(2))
        .thenCancel()
        .verify(VERIFY_TIMEOUT)
  }

  /** The released range can shrink, or the questionnaire be unpublished, while subscribed. */
  @Test
  fun shouldStopEmittingContentOnceNoLongerReadable() {
    Mockito.`when`(permissionEvaluator.hasPermission(authentication, releasedContent, "read"))
        .thenReturn(true, false)

    StepVerifier.withVirtualTime { controller.questionnaireContentUpdated(QUESTIONNAIRE_ID) }
        .expectSubscription()
        .then {
          SecurityContextHolder.clearContext()
          publishAnswerChanged(releasedContent)
        }
        .thenAwait(SAMPLE_INTERVAL)
        .expectNext(releasedContent)
        .then { publishAnswerChanged(releasedContent) }
        .expectNoEvent(SAMPLE_INTERVAL.multipliedBy(2))
        .thenCancel()
        .verify(VERIFY_TIMEOUT)
  }

  private fun publishAnswerChanged(content: Content) {
    eventPublisher.publishAnswerChanged(
        AnswerChangedEvent(
            roundId = UUID.randomUUID(),
            contentId = content.id!!,
            questionnaireId = QUESTIONNAIRE_ID))
  }
}
