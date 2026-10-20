/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.questionnaire.api

import java.time.Duration
import java.util.Optional
import java.util.UUID
import net.particify.arsnova.core4.questionnaire.questionnaire.Questionnaire
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnaireServiceImpl
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.security.access.PermissionEvaluator
import org.springframework.security.authentication.TestingAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import reactor.test.StepVerifier

private val SUBSCRIBED_ROOM_ID = UUID.randomUUID()
private val OTHER_ROOM_ID = UUID.randomUUID()
private val QUESTIONNAIRE_ID = UUID.randomUUID()
private val NO_EVENT_WINDOW = Duration.ofMillis(200)
private val VERIFY_TIMEOUT = Duration.ofSeconds(10)

class QuestionnaireSubscriptionControllerTest {
  private val eventPublisher = QuestionnaireEventPublisher()
  private val questionnaireServiceImpl = Mockito.mock(QuestionnaireServiceImpl::class.java)
  private val permissionEvaluator = Mockito.mock(PermissionEvaluator::class.java)
  private val authentication = TestingAuthenticationToken("participant", null, "ROLE_USER")
  private val controller =
      QuestionnaireSubscriptionController(
          eventPublisher, questionnaireServiceImpl, permissionEvaluator)

  @AfterEach
  fun clearAuthentication() {
    SecurityContextHolder.clearContext()
  }

  /**
   * The questionnaire can be unpublished while subscribed. Emissions run on another thread without
   * the subscriber's security context, so this also covers the authentication being captured.
   */
  @Test
  fun shouldStopEmittingUpdatesOnceQuestionnaireIsNoLongerReadable() {
    val questionnaire = Questionnaire(id = QUESTIONNAIRE_ID, roomId = SUBSCRIBED_ROOM_ID)
    Mockito.`when`(questionnaireServiceImpl.findById(QUESTIONNAIRE_ID))
        .thenReturn(Optional.of(questionnaire))
    Mockito.`when`(permissionEvaluator.hasPermission(authentication, questionnaire, "read"))
        .thenReturn(true, false)
    SecurityContextHolder.getContext().authentication = authentication
    val updates = controller.questionnaireUpdated(QUESTIONNAIRE_ID)
    SecurityContextHolder.clearContext()

    StepVerifier.create(updates)
        .expectSubscription()
        .then { eventPublisher.publishQuestionnaireUpdated(QUESTIONNAIRE_ID) }
        .expectNext(questionnaire)
        .then { eventPublisher.publishQuestionnaireUpdated(QUESTIONNAIRE_ID) }
        .expectNoEvent(NO_EVENT_WINDOW)
        .thenCancel()
        .verify(VERIFY_TIMEOUT)
  }

  @Test
  fun shouldEmitIdForChangeInSubscribedRoom() {
    StepVerifier.create(controller.questionnairesChanged(SUBSCRIBED_ROOM_ID))
        .expectSubscription()
        .then {
          eventPublisher.publishQuestionnairesChanged(
              Questionnaire(id = QUESTIONNAIRE_ID, roomId = SUBSCRIBED_ROOM_ID))
        }
        .expectNext(QUESTIONNAIRE_ID)
        .thenCancel()
        .verify(VERIFY_TIMEOUT)
  }

  @Test
  fun shouldNotEmitForChangeInOtherRoom() {
    StepVerifier.create(controller.questionnairesChanged(SUBSCRIBED_ROOM_ID))
        .expectSubscription()
        .then {
          eventPublisher.publishQuestionnairesChanged(
              Questionnaire(id = QUESTIONNAIRE_ID, roomId = OTHER_ROOM_ID))
        }
        .expectNoEvent(NO_EVENT_WINDOW)
        .thenCancel()
        .verify(VERIFY_TIMEOUT)
  }

  @Test
  fun shouldNotEmitForUnpersistedQuestionnaire() {
    StepVerifier.create(controller.questionnairesChanged(SUBSCRIBED_ROOM_ID))
        .expectSubscription()
        .then {
          eventPublisher.publishQuestionnairesChanged(Questionnaire(roomId = SUBSCRIBED_ROOM_ID))
        }
        .expectNoEvent(NO_EVENT_WINDOW)
        .thenCancel()
        .verify(VERIFY_TIMEOUT)
  }
}
