/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.qna.internal

import jakarta.transaction.Transactional
import java.util.UUID
import net.particify.arsnova.core4.qna.Tag
import net.particify.arsnova.core4.qna.event.TagCreatedEvent
import net.particify.arsnova.core4.qna.event.TagDeletedEvent
import net.particify.arsnova.core4.qna.event.TagsDeletedEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service

@Service
class TagServiceImpl(
    private val tagRepository: TagRepository,
    private val applicationEventPublisher: ApplicationEventPublisher
) {
  fun create(tag: Tag): Tag {
    val tag = tagRepository.save(tag)
    applicationEventPublisher.publishEvent(TagCreatedEvent(tag.id!!))
    return tag
  }

  fun delete(id: UUID) {
    applicationEventPublisher.publishEvent(TagDeletedEvent(id))
    tagRepository.deleteById(id)
  }

  @Transactional
  fun deleteTagsByQnaId(id: UUID): Int {
    val count = tagRepository.deleteByQnaId(id)
    applicationEventPublisher.publishEvent(TagsDeletedEvent(id, count))
    return count
  }

  @Transactional
  fun duplicateForQna(originalQnaId: UUID, duplicatedQnaId: UUID): List<Tag> {
    val tags = findByQnaId(originalQnaId)
    val newTags = mutableListOf<Tag>()
    tags.forEach {
      val newTag = it.copy(duplicatedQnaId)
      val newTagPersisted = tagRepository.save(newTag)
      newTags.add(newTagPersisted)
      applicationEventPublisher.publishEvent(TagCreatedEvent(newTagPersisted.id!!))
    }
    return newTags
  }

  fun findByQnaId(qnaId: UUID): List<Tag> {
    return tagRepository.findByQnaId(qnaId)
  }
}
