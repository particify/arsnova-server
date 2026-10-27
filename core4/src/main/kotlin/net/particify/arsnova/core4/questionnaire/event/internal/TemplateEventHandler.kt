/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.event.internal

import java.time.Instant
import net.particify.arsnova.core4.questionnaire.template.RoomlessQuestionnaireChangedEvent
import net.particify.arsnova.core4.questionnaire.template.TemplateServiceImpl
import net.particify.arsnova.core4.user.event.UserDeletedEvent
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
class TemplateEventHandler(private val templateServiceImpl: TemplateServiceImpl) {
  private val logger = LoggerFactory.getLogger(this::class.java)

  /**
   * Published templates are kept for others to use, as are questionnaires copied from the user's
   * templates into rooms. Users are soft-deleted, so the creator reference stays valid.
   */
  @EventListener
  fun handleUserDeleted(event: UserDeletedEvent) {
    logger.debug("Deleting unpublished templates due to user deletion: {}", event)
    templateServiceImpl.deleteUnpublishedByCreator(event.id)
  }

  /**
   * Marks the template as changed for the list of the creator's templates. Nothing to mark while a
   * template is still being created from its questionnaire.
   */
  @EventListener
  fun handleRoomlessQuestionnaireChanged(event: RoomlessQuestionnaireChangedEvent) {
    val template = templateServiceImpl.findByIdOrNull(event.id) ?: return
    template.auditMetadata.updatedAt = Instant.now()
    templateServiceImpl.save(template)
  }
}
