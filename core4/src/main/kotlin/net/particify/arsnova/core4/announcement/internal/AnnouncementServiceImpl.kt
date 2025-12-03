/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.announcement.internal

import net.particify.arsnova.core4.announcement.Announcement
import org.springframework.stereotype.Service

@Service
class AnnouncementServiceImpl(private val announcementRepository: AnnouncementRepository) :
    AnnouncementRepository by announcementRepository {
  @Deprecated(
      "Deprecated by base implementation", replaceWith = ReplaceWith("deleteAllInBatch(entities)"))
  override fun deleteInBatch(entities: Iterable<Announcement>) =
      announcementRepository.deleteInBatch(entities)
}
