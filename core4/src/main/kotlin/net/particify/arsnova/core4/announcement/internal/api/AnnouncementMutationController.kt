/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.announcement.internal.api

import java.util.UUID
import net.particify.arsnova.core4.announcement.Announcement
import net.particify.arsnova.core4.announcement.exception.AnnouncementNotFoundException
import net.particify.arsnova.core4.announcement.internal.AnnouncementServiceImpl
import org.springframework.data.repository.findByIdOrNull
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

@Controller
@PreAuthorize("hasRole('USER')")
@SchemaMapping(typeName = "Mutation")
class AnnouncementMutationController(private val announcementService: AnnouncementServiceImpl) {
  @MutationMapping
  @PreAuthorize("hasPermission(#input.roomId, 'Room', 'write')")
  fun createAnnouncement(@Argument input: CreateAnnouncementInput): Announcement {
    return announcementService.save(input.toAnnouncement())
  }

  @MutationMapping
  @PreAuthorize("hasPermission(#input.id, 'Announcement', 'write')")
  fun updateAnnouncement(@Argument input: UpdateAnnouncementInput): Announcement {
    val announcement =
        announcementService.findByIdOrNull(input.id)
            ?: throw AnnouncementNotFoundException(input.id)
    if (input.title != null) {
      announcement.title = input.title
    }
    if (input.body != null) {
      announcement.body = input.body
    }
    return announcementService.save(announcement)
  }

  @MutationMapping
  @PreAuthorize("hasPermission(#id, 'Announcement', 'delete')")
  fun deleteAnnouncement(@Argument id: UUID): UUID {
    announcementService.deleteById(id)
    return id
  }
}
