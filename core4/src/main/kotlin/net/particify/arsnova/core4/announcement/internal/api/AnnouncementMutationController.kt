/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.announcement.internal.api

import java.util.UUID
import net.particify.arsnova.core4.announcement.Announcement
import net.particify.arsnova.core4.announcement.exception.AnnouncementNotFoundException
import net.particify.arsnova.core4.announcement.internal.AnnouncementRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.stereotype.Controller

@Controller
@SchemaMapping(typeName = "Mutation")
class AnnouncementMutationController(private val announcementRepository: AnnouncementRepository) {
  @MutationMapping
  fun createAnnouncement(@Argument input: CreateAnnouncementInput): Announcement {
    return announcementRepository.save(input.toAnnouncement())
  }

  @MutationMapping
  fun updateAnnouncement(@Argument input: UpdateAnnouncementInput): Announcement {
    val announcement =
        announcementRepository.findByIdOrNull(input.id)
            ?: throw AnnouncementNotFoundException(input.id)
    if (input.title != null) {
      announcement.title = input.title
    }
    if (input.body != null) {
      announcement.body = input.body
    }
    return announcementRepository.save(announcement)
  }

  @MutationMapping
  fun deleteAnnouncement(@Argument id: UUID): UUID {
    announcementRepository.deleteById(id)
    return id
  }
}
