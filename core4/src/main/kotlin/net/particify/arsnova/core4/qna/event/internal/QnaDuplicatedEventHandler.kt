/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.qna.event.internal

import net.particify.arsnova.core4.qna.event.QnaDuplicatedEvent
import net.particify.arsnova.core4.qna.internal.PostServiceImpl
import net.particify.arsnova.core4.qna.internal.TagServiceImpl
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class QnaDuplicatedEventHandler(
    private val postServiceImpl: PostServiceImpl,
    private val tagServiceImpl: TagServiceImpl
) {
  private val logger = LoggerFactory.getLogger(this::class.java)

  @EventListener
  fun handleQnaDuplicated(event: QnaDuplicatedEvent) {
    logger.debug("Duplicate posts due to qna duplication: {}", event)
    val duplicatedTags = tagServiceImpl.duplicateForQna(event.originalQnaId, event.duplicatedQnaId)
    postServiceImpl.duplicateForQna(event.originalQnaId, event.duplicatedQnaId, duplicatedTags)
  }
}
