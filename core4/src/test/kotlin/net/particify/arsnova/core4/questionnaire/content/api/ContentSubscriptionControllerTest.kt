/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.content.api

import java.time.Duration
import java.util.Optional
import java.util.UUID
import net.particify.arsnova.core4.questionnaire.content.Content
import net.particify.arsnova.core4.questionnaire.content.ContentPermissionEvaluator
import net.particify.arsnova.core4.questionnaire.content.ContentRepository
import net.particify.arsnova.core4.questionnaire.questionnaire.Questionnaire
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnairePublishingMode
import net.particify.arsnova.core4.questionnaire.round.api.AnswerChangedEvent
import net.particify.arsnova.core4.questionnaire.round.api.RoundStatsEventPublisher
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.dao.TransientDataAccessResourceException
import org.springframework.security.access.PermissionEvaluator
import org.springframework.security.authentication.TestingAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import reactor.core.Disposable
import reactor.test.StepVerifier

private val QUESTIONNAIRE_ID = UUID.randomUUID()

// Must match STATS_UPDATE_SAMPLE_INTERVAL of the controller under test.
private val SAMPLE_INTERVAL = Duration.ofSeconds(3)
private val VERIFY_TIMEOUT = Duration.ofSeconds(10)

/**
 * Emissions run without the subscriber's security context, so each test clears it before the first
 * answer: the controller has to decide everything subscriber-specific at subscription.
 */
class ContentSubscriptionControllerTest {
  private val eventPublisher = RoundStatsEventPublisher()
  private val contentRepository = Mockito.mock(ContentRepository::class.java)
  private val permissionEvaluator = Mockito.mock(PermissionEvaluator::class.java)
  private val participant = TestingAuthenticationToken("participant", null, "ROLE_USER")
  private val moderator = TestingAuthenticationToken("moderator", null, "ROLE_USER")
  private val controller =
      ContentSubscriptionController(
          eventPublisher,
          contentRepository,
          ContentPermissionEvaluator(contentRepository),
          permissionEvaluator)
  private val questionnaire =
      Questionnaire(
          id = QUESTIONNAIRE_ID,
          published = true,
          publishingMode = QuestionnairePublishingMode.UP_TO)
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
    questionnaire.publishedUpToContent = releasedContent
    Mockito.`when`(
            permissionEvaluator.hasPermission(
                moderator, QUESTIONNAIRE_ID, "Questionnaire", "moderate"))
        .thenReturn(true)
    SecurityContextHolder.getContext().authentication = participant
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
        .expectNext(releasedContent)
        .thenCancel()
        .verify(VERIFY_TIMEOUT)
  }

  /** The presenter can answer, or ban a keyword of, a content not yet released to participants. */
  @Test
  fun shouldNotEmitUnreleasedContentToParticipant() {
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

  @Test
  fun shouldEmitUnreleasedContentToModerator() {
    SecurityContextHolder.getContext().authentication = moderator

    StepVerifier.withVirtualTime { controller.questionnaireContentUpdated(QUESTIONNAIRE_ID) }
        .expectSubscription()
        .then {
          SecurityContextHolder.clearContext()
          publishAnswerChanged(unreleasedContent)
        }
        .expectNext(unreleasedContent)
        .thenCancel()
        .verify(VERIFY_TIMEOUT)
  }

  /**
   * The released range can shrink, or the questionnaire be unpublished, while subscribed. The
   * permission is decided at subscription only, never for an emission.
   */
  @Test
  fun shouldStopEmittingContentOnceNoLongerReleased() {
    StepVerifier.withVirtualTime { controller.questionnaireContentUpdated(QUESTIONNAIRE_ID) }
        .expectSubscription()
        .then {
          SecurityContextHolder.clearContext()
          publishAnswerChanged(releasedContent)
        }
        .expectNext(releasedContent)
        .then {
          questionnaire.publishedUpToContent = null
          publishAnswerChanged(releasedContent)
        }
        .expectNoEvent(SAMPLE_INTERVAL.multipliedBy(2))
        .thenCancel()
        .verify(VERIFY_TIMEOUT)

    Mockito.verify(permissionEvaluator)
        .hasPermission(participant, QUESTIONNAIRE_ID, "Questionnaire", "moderate")
    Mockito.verifyNoMoreInteractions(permissionEvaluator)
  }

  @Test
  fun shouldShareOneComputationAmongSubscribers() {
    val second = mutableListOf<Content>()
    var secondSubscription: Disposable? = null
    StepVerifier.withVirtualTime { controller.questionnaireContentUpdated(QUESTIONNAIRE_ID) }
        .expectSubscription()
        .then {
          secondSubscription =
              controller.questionnaireContentUpdated(QUESTIONNAIRE_ID).subscribe { second.add(it) }
          publishAnswerChanged(releasedContent)
          publishAnswerChanged(releasedContent)
        }
        .expectNext(releasedContent)
        .thenAwait(SAMPLE_INTERVAL)
        .expectNext(releasedContent)
        .then { secondSubscription?.dispose() }
        .thenCancel()
        .verify(VERIFY_TIMEOUT)

    Assertions.assertEquals(listOf(releasedContent, releasedContent), second)
    Mockito.verify(contentRepository, Mockito.times(2)).findById(releasedContent.id!!)
  }

  /** The content replayed to a late joiner is shared, so it has to pass the joiner's gate. */
  @Test
  fun shouldHandALateSubscriberTheLastContentOnlyIfPermitted() {
    SecurityContextHolder.getContext().authentication = moderator
    val late = mutableListOf<Content>()
    StepVerifier.withVirtualTime { controller.questionnaireContentUpdated(QUESTIONNAIRE_ID) }
        .expectSubscription()
        .then { publishAnswerChanged(unreleasedContent) }
        .expectNext(unreleasedContent)
        .then {
          SecurityContextHolder.getContext().authentication = participant
          val unpermitted =
              controller.questionnaireContentUpdated(QUESTIONNAIRE_ID).subscribe { late.add(it) }
          publishAnswerChanged(unreleasedContent)
          Assertions.assertEquals(emptyList<Content>(), late)
          unpermitted.dispose()
          questionnaire.publishedUpToContent = unreleasedContent
          controller
              .questionnaireContentUpdated(QUESTIONNAIRE_ID)
              .subscribe { late.add(it) }
              .also { publishAnswerChanged(unreleasedContent) }
              .dispose()
        }
        .thenCancel()
        .verify(VERIFY_TIMEOUT)

    Assertions.assertEquals(listOf(unreleasedContent), late)
    Mockito.verify(contentRepository, Mockito.times(1)).findById(unreleasedContent.id!!)
  }

  @Test
  fun shouldSkipAContentWhichFailedToLoadAndKeepServingTheOthers() {
    SecurityContextHolder.getContext().authentication = moderator
    Mockito.`when`(contentRepository.findById(unreleasedContent.id!!))
        .thenThrow(TransientDataAccessResourceException("Connection lost"))
        .thenReturn(Optional.of(unreleasedContent))

    StepVerifier.withVirtualTime { controller.questionnaireContentUpdated(QUESTIONNAIRE_ID) }
        .expectSubscription()
        .then {
          SecurityContextHolder.clearContext()
          publishAnswerChanged(unreleasedContent)
          publishAnswerChanged(releasedContent)
        }
        .expectNext(releasedContent)
        .then { publishAnswerChanged(unreleasedContent) }
        .expectNext(unreleasedContent)
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
