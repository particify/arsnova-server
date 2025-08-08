/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.qna.event.internal

import net.particify.arsnova.core4.qna.event.PostDeletedEvent
import net.particify.arsnova.core4.qna.internal.ReplyServiceImpl
import net.particify.arsnova.core4.qna.internal.VoteServiceImpl
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class PostDeletedEventHandler(
    private val replyServiceImpl: ReplyServiceImpl,
    private val voteServiceImpl: VoteServiceImpl
) {
  private val logger = LoggerFactory.getLogger(this::class.java)

  @EventListener
  fun handlePostDeleted(event: PostDeletedEvent) {
    logger.debug("Deleting replies and votes due to post deletion: {}", event)
    replyServiceImpl.deleteByPostId(event.id)
    voteServiceImpl.deleteByPostId(event.id)
  }
}
