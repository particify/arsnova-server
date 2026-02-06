/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.room.event

import java.util.UUID
import org.jmolecules.event.annotation.DomainEvent
import org.jmolecules.event.annotation.Externalized

const val ROOM_DUPLICATED_DESTINATION = "backend.event.room.duplicated"

@DomainEvent
@Externalized(target = ROOM_DUPLICATED_DESTINATION)
data class RoomDuplicatedEvent(val originalRoomId: UUID, val duplicatedRoomId: UUID) : RoomEvent
