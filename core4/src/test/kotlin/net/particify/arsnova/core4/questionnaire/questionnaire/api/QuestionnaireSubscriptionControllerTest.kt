/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.questionnaire.api

import java.time.Duration
import java.util.UUID
import net.particify.arsnova.core4.questionnaire.questionnaire.Questionnaire
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnaireServiceImpl
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import reactor.test.StepVerifier

private val SUBSCRIBED_ROOM_ID = UUID.randomUUID()
private val OTHER_ROOM_ID = UUID.randomUUID()
private val QUESTIONNAIRE_ID = UUID.randomUUID()
private val NO_EVENT_WINDOW = Duration.ofMillis(200)
private val VERIFY_TIMEOUT = Duration.ofSeconds(10)

class QuestionnaireSubscriptionControllerTest {
  private val eventPublisher = QuestionnaireEventPublisher()
  private val controller =
      QuestionnaireSubscriptionController(
          eventPublisher, Mockito.mock(QuestionnaireServiceImpl::class.java))

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
