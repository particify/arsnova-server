/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.qna.internal

import jakarta.transaction.Transactional
import java.util.UUID
import net.particify.arsnova.core4.qna.Reply
import net.particify.arsnova.core4.qna.event.RepliesDeletedEvent
import net.particify.arsnova.core4.qna.event.ReplyCreatedEvent
import net.particify.arsnova.core4.qna.event.ReplyDeletedEvent
import net.particify.arsnova.core4.qna.exception.ReplyNotFoundException
import net.particify.arsnova.core4.qna.internal.api.PostEventPublisher
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.repository.findByIdOrNull
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
  fun update(id: UUID, body: String): Reply {
    val reply = replyRepository.findByIdOrNull(id) ?: throw ReplyNotFoundException(id)
    reply.body = body
    val persistedReply = replyRepository.save(reply)
    // Subscribers key the reply by its ID, so re-emitting it under the same ID patches their cache.
    postEventPublisher.publishReplyCreate(reply.post!!.id!!, persistedReply)
    return persistedReply
  }

  @Transactional
  fun delete(id: UUID) {
    val reply = replyRepository.findByIdOrNull(id) ?: throw ReplyNotFoundException(id)
    replyRepository.delete(reply)
    applicationEventPublisher.publishEvent(ReplyDeletedEvent(id))
  }

  @Transactional
  fun deleteByPostId(postId: UUID): Int {
    val count = replyRepository.deleteByPostId(postId)
    applicationEventPublisher.publishEvent(RepliesDeletedEvent(postId, count))
    return count
  }

  @Transactional
  fun duplicateForPost(originalPostId: UUID, duplicatedPostId: UUID) {
    val replies = replyRepository.findByPostId(originalPostId)
    replies.forEach {
      val newReply = it.copy(duplicatedPostId)
      val newReplyPersisted = replyRepository.save(newReply)
      applicationEventPublisher.publishEvent(ReplyCreatedEvent(newReplyPersisted.id!!))
    }
  }
}
