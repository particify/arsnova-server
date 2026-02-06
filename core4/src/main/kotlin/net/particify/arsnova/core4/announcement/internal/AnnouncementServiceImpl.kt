/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.announcement.internal

import java.util.UUID
import net.particify.arsnova.core4.announcement.Announcement
import net.particify.arsnova.core4.announcement.event.AnnouncementCreatedEvent
import net.particify.arsnova.core4.announcement.event.AnnouncementDeletedEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service

@Service
class AnnouncementServiceImpl(
    private val announcementRepository: AnnouncementRepository,
    val applicationEventPublisher: ApplicationEventPublisher
) : AnnouncementRepository by announcementRepository {
  override fun <S : Announcement> save(announcement: S): S {
    val persistedAnnouncement = announcementRepository.save(announcement)
    applicationEventPublisher.publishEvent(AnnouncementCreatedEvent(persistedAnnouncement.id!!))
    return persistedAnnouncement
  }

  override fun deleteById(id: UUID) {
    announcementRepository.deleteById(id)
    applicationEventPublisher.publishEvent(AnnouncementDeletedEvent(id))
  }

  @Deprecated(
      "Deprecated by base implementation", replaceWith = ReplaceWith("deleteAllInBatch(entities)"))
  override fun deleteInBatch(entities: Iterable<Announcement>) =
      announcementRepository.deleteInBatch(entities)
}
