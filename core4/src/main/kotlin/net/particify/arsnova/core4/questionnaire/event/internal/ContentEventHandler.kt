/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.event.internal

import net.particify.arsnova.core4.questionnaire.event.ContentCreatedEvent
import net.particify.arsnova.core4.questionnaire.event.ContentDeletedEvent
import net.particify.arsnova.core4.questionnaire.event.ContentDuplicatedEvent
import net.particify.arsnova.core4.questionnaire.round.RoundServiceImpl
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class ContentEventHandler(private val roundServiceImpl: RoundServiceImpl) {
  private val logger = LoggerFactory.getLogger(this::class.java)

  @EventListener
  fun handleContentDeleted(event: ContentDeletedEvent) {
    logger.debug("Deleting rounds due to content deletion: {}", event)
    roundServiceImpl.deleteByContentId(event.id)
  }

  @EventListener
  fun handleContentCreated(event: ContentCreatedEvent) {
    logger.debug("Creating round due to content creation: {}", event)
    //    val content = Content(id = event.id)
    //    val round = Round(content = content)
    //    val persistedRound = roundServiceImpl.save(round)
    //    content.activeRound = persistedRound
    //    contentServiceImpl.save(content)
  }

  @EventListener
  fun handleContentDuplicated(event: ContentDuplicatedEvent) {
    logger.debug("Duplicate answers due to post duplication: {}", event)
  }
}
