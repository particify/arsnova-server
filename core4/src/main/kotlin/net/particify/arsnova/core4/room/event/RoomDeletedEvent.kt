/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.room.event

import java.util.UUID
import net.particify.arsnova.core4.common.event.EntityDeletedEvent
import org.jmolecules.event.annotation.DomainEvent
import org.jmolecules.event.annotation.Externalized

const val ROOM_DELETED_DESTINATION = "backend.event.room.afterdeletion"

@DomainEvent
@Externalized(target = ROOM_DELETED_DESTINATION)
data class RoomDeletedEvent(override val id: UUID) : RoomEvent, EntityDeletedEvent<UUID>
