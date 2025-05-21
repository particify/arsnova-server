/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.announcement.internal.api

import java.util.UUID
import net.particify.arsnova.core4.announcement.Announcement
import net.particify.arsnova.core4.room.Room

data class CreateAnnouncementInput(val roomId: UUID, val title: String, val body: String) {
  fun toAnnouncement() = Announcement(title = title, body = body, room = Room(id = roomId))
}
