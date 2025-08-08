/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.qna.event.internal

import net.particify.arsnova.core4.qna.event.QnaDeletedEvent
import net.particify.arsnova.core4.qna.internal.PostServiceImpl
import net.particify.arsnova.core4.qna.internal.TagServiceImpl
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class QnaDeletedEventHandler(
    private val postServiceImpl: PostServiceImpl,
    private val tagServiceImpl: TagServiceImpl,
) {
  private val logger = LoggerFactory.getLogger(this::class.java)

  @EventListener
  fun handleQnaDeleted(event: QnaDeletedEvent) {
    logger.debug("Deleting posts and tags due to qna deletion: {}", event)
    postServiceImpl.deletePostsByQnaId(event.id)
    tagServiceImpl.deleteTagsByQnaId(event.id)
  }
}
