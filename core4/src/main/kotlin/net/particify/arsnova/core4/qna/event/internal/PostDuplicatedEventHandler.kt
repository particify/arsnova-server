/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.qna.event.internal

import net.particify.arsnova.core4.qna.event.PostDuplicatedEvent
import net.particify.arsnova.core4.qna.internal.ReplyServiceImpl
import net.particify.arsnova.core4.qna.internal.VoteServiceImpl
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class PostDuplicatedEventHandler(
    private val replyServiceImpl: ReplyServiceImpl,
    private val voteServiceImpl: VoteServiceImpl
) {
  private val logger = LoggerFactory.getLogger(this::class.java)

  @EventListener
  fun handlePostDuplicated(event: PostDuplicatedEvent) {
    logger.debug("Duplicate replies and votes due to post duplication: {}", event)
    replyServiceImpl.duplicateForPost(event.originalPostId, event.duplicatedPostId)
    voteServiceImpl.duplicateForPost(event.originalPostId, event.duplicatedPostId)
  }
}
