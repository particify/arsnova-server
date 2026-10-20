/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.event.internal

import net.particify.arsnova.core4.questionnaire.content.ContentServiceImpl
import net.particify.arsnova.core4.questionnaire.event.QuestionnaireDeletedEvent
import net.particify.arsnova.core4.questionnaire.event.QuestionnaireDuplicatedEvent
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnaireServiceImpl
import net.particify.arsnova.core4.room.event.RoomDeletedEvent
import net.particify.arsnova.core4.room.event.RoomDuplicatedEvent
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class QuestionnaireEventHandler(
    private val contentServiceImpl: ContentServiceImpl,
    private val questionnaireServiceImpl: QuestionnaireServiceImpl,
) {
  private val logger = LoggerFactory.getLogger(this::class.java)

  @EventListener
  fun handleQuestionnaireDeleted(event: QuestionnaireDeletedEvent) {
    logger.debug("Deleting contents due to questionnaire deletion: {}", event)
    contentServiceImpl.deleteByQuestionnaireId(event.id)
  }

  @EventListener
  fun handleQuestionnaireDuplicated(event: QuestionnaireDuplicatedEvent) {
    logger.debug("Questionnaire duplicated: {}", event)
  }

  @EventListener
  fun handleRoomDeleted(event: RoomDeletedEvent) {
    logger.debug("Deleting questionnaires due to room deletion: {}", event)
    // Runs synchronously inside the room deletion's transaction, before the room row is removed,
    // so the questionnaire's room foreign key never sees a missing room.
    questionnaireServiceImpl.deleteByRoomId(event.id)
  }

  @EventListener
  fun handleRoomDuplicated(event: RoomDuplicatedEvent) {
    logger.debug("Duplicate questionnaires due to room duplication: {}", event)
    questionnaireServiceImpl.duplicateForRoom(event.originalRoomId, event.duplicatedRoomId)
  }
}
