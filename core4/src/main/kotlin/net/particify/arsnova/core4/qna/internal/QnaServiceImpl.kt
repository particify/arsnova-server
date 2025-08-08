/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.qna.internal

import jakarta.transaction.Transactional
import java.util.UUID
import net.particify.arsnova.core4.qna.Qna
import net.particify.arsnova.core4.qna.QnaService
import net.particify.arsnova.core4.qna.QnaState
import net.particify.arsnova.core4.qna.event.QnaCreatedEvent
import net.particify.arsnova.core4.qna.event.QnaDeletedEvent
import net.particify.arsnova.core4.qna.exception.QnaNotFoundException
import net.particify.arsnova.core4.qna.internal.api.QnaEventPublisher
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.ScrollPosition
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class QnaServiceImpl(
    private val qnaRepository: QnaRepository,
    private val postRepository: PostRepository,
    private val qnaEventPublisher: QnaEventPublisher,
    private val applicationEventPublisher: ApplicationEventPublisher
) : QnaService {

  @Transactional
  override fun create(roomId: UUID, topic: String?) {
    val qna = qnaRepository.save(Qna(roomId = roomId, topic = topic))
    applicationEventPublisher.publishEvent(QnaCreatedEvent(qna.id!!))
  }

  @Transactional
  fun start(id: UUID): Qna {
    val qna = qnaRepository.findByIdOrNull(id) ?: throw QnaNotFoundException(id)
    qna.state = QnaState.STARTED
    val persistedQna = qnaRepository.save(qna)
    qnaEventPublisher.publishQnaStateChanged(persistedQna)
    return persistedQna
  }

  @Transactional
  fun pause(id: UUID): Qna {
    val qna = qnaRepository.findByIdOrNull(id) ?: throw QnaNotFoundException(id)
    qna.state = QnaState.PAUSED
    val persistedQna = qnaRepository.save(qna)
    qnaEventPublisher.publishQnaStateChanged(persistedQna)
    return persistedQna
  }

  @Transactional
  fun stop(id: UUID): Qna {
    val qna = qnaRepository.findByIdOrNull(id) ?: throw QnaNotFoundException(id)
    qna.state = QnaState.STOPPED
    val persistedQna = qnaRepository.save(qna)
    qnaEventPublisher.publishQnaStateChanged(persistedQna)
    return persistedQna
  }

  @Transactional
  fun updateThreshold(id: UUID, threshold: Int?): Qna {
    val qna = qnaRepository.findByIdOrNull(id) ?: throw QnaNotFoundException(id)
    qna.threshold = threshold
    return qnaRepository.save(qna)
  }

  @Transactional
  fun updateAutoPublish(id: UUID, autoPublish: Boolean): Qna {
    val qna = qnaRepository.findByIdOrNull(id) ?: throw QnaNotFoundException(id)
    qna.autoPublish = autoPublish
    return qnaRepository.save(qna)
  }

  @Transactional
  fun updateActivePost(id: UUID, activePostId: UUID?): Qna {
    val qna = qnaRepository.findByIdOrNull(id) ?: throw QnaNotFoundException(id)
    if (activePostId != null) {
      val post = postRepository.findByIdOrNull(activePostId) ?: error("Post not found.")
      qna.activePost = post
    } else {
      qna.activePost = null
    }
    val persistedQna = qnaRepository.save(qna)
    qnaEventPublisher.publishUpdatedActivePost(persistedQna)
    return persistedQna
  }

  @Transactional
  fun deleteByRoomId(roomId: UUID) {
    val qnaIds = qnaRepository.findByRoomId(roomId, ScrollPosition.offset()).content.map { it.id!! }
    qnaIds.forEach {
      updateActivePost(it, null)
      applicationEventPublisher.publishEvent(QnaDeletedEvent(it))
      qnaRepository.deleteById(it)
    }
  }
}
