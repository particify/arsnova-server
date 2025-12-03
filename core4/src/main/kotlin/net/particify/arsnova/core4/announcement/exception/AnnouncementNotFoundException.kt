/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.announcement.exception

import java.util.UUID

class AnnouncementNotFoundException(val id: UUID) : RuntimeException("Announcement not found")
