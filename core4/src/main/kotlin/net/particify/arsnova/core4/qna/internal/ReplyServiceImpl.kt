/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.qna.internal

import jakarta.transaction.Transactional
import java.util.UUID
import net.particify.arsnova.core4.qna.Reply
import net.particify.arsnova.core4.qna.event.RepliesDeletedEvent
import net.particify.arsnova.core4.qna.event.ReplyCreatedEvent
import net.particify.arsnova.core4.qna.internal.api.PostEventPublisher
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service

@Service
class ReplyServiceImpl(
    private val replyRepository: ReplyRepository,
    private val postEventPublisher: PostEventPublisher,
    private val applicationEventPublisher: ApplicationEventPublisher
) {

  @Transactional
  fun create(reply: Reply): Reply {
    val persistedReply = replyRepository.save(reply)
    postEventPublisher.publishReplyCreate(reply.post!!.id!!, persistedReply)
    applicationEventPublisher.publishEvent(ReplyCreatedEvent(persistedReply.id!!))
    return persistedReply
  }

  @Transactional
  fun deleteByPostId(postId: UUID): Int {
    val count = replyRepository.deleteByPostId(postId)
    applicationEventPublisher.publishEvent(RepliesDeletedEvent(postId, count))
    return count
  }

  fun update(reply: Reply): Reply = replyRepository.save(reply)

  fun delete(id: UUID) = replyRepository.deleteById(id)
}
