/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.questionnaire.api

import java.util.UUID
import net.particify.arsnova.core4.questionnaire.questionnaire.Questionnaire
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import reactor.util.concurrent.Queues

data class QuestionnairesChangedEvent(val roomId: UUID, val questionnaireId: UUID)

@Component
class QuestionnaireEventPublisher {

  private val logger = LoggerFactory.getLogger(javaClass)

  private val questionnaireUpdatedSink =
      Sinks.many().multicast().onBackpressureBuffer<UUID>(Queues.SMALL_BUFFER_SIZE, false)

  private val questionnairesChangedSink =
      Sinks.many()
          .multicast()
          .onBackpressureBuffer<QuestionnairesChangedEvent>(Queues.SMALL_BUFFER_SIZE, false)

  fun publishQuestionnaireUpdated(questionnaireId: UUID) {
    afterCommit { emit(questionnaireUpdatedSink, questionnaireId) }
  }

  /**
   * Signals that the room's list of questionnaires, or a questionnaire's entry in it, changed. The
   * ids are read immediately, so this also works for a questionnaire about to be deleted.
   */
  fun publishQuestionnairesChanged(questionnaire: Questionnaire) {
    val roomId = questionnaire.roomId ?: return
    val questionnaireId = questionnaire.id ?: return
    afterCommit {
      emit(questionnairesChangedSink, QuestionnairesChangedEvent(roomId, questionnaireId))
    }
  }

  private fun afterCommit(action: () -> Unit) {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(
          object : TransactionSynchronization {
            override fun afterCommit() = action()
          })
    } else {
      action()
    }
  }

  private fun <T : Any> emit(sink: Sinks.Many<T>, event: T) {
    val result = sink.tryEmitNext(event)
    if (result.isFailure) logger.warn("Dropped questionnaire event {}: {}", event, result)
  }

  fun questionnaireUpdatedFlux(): Flux<UUID> {
    return questionnaireUpdatedSink.asFlux().doOnError { logger.error(it.message) }
  }

  fun questionnairesChangedFlux(): Flux<QuestionnairesChangedEvent> {
    return questionnairesChangedSink.asFlux().doOnError { logger.error(it.message) }
  }
}
