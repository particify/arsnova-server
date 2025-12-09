/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.room.event

import java.util.UUID
import org.jmolecules.event.annotation.Externalized

const val ROOM_DELETED_DESTINATION = "backend.event.room.afterdeletion"

@Externalized(target = ROOM_DELETED_DESTINATION) data class RoomDeletedEvent(val id: UUID)
