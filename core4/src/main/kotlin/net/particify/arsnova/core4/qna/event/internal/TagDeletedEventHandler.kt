/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.qna.event.internal

import net.particify.arsnova.core4.qna.event.TagDeletedEvent
import net.particify.arsnova.core4.qna.internal.PostServiceImpl
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class TagDeletedEventHandler(private val postServiceImpl: PostServiceImpl) {
  private val logger = LoggerFactory.getLogger(this::class.java)

  @EventListener
  fun handleTagDeleted(event: TagDeletedEvent) {
    logger.debug("Deleting tags from posts due to tag deletion: {}", event)
    postServiceImpl.removeTagFromPosts(event.id)
  }
}
