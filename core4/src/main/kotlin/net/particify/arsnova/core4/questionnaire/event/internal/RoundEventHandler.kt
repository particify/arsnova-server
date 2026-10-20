/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.event.internal

import net.particify.arsnova.core4.questionnaire.event.RoundDeletedEvent
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class RoundEventHandler {
  private val logger = LoggerFactory.getLogger(this::class.java)

  @EventListener
  fun handleRoundDeleted(event: RoundDeletedEvent) {
    logger.debug("Deleting answers due to round deletion: {}", event)
  }
}
