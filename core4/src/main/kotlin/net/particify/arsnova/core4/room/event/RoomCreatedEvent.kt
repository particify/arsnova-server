/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.room.event

import java.util.UUID
import net.particify.arsnova.core4.common.event.EntityCreatedEvent
import org.jmolecules.event.annotation.DomainEvent
import org.jmolecules.event.annotation.Externalized

const val ROOM_CREATED_DESTINATION = "backend.event.room.aftercreation"

@DomainEvent
@Externalized(target = ROOM_CREATED_DESTINATION)
data class RoomCreatedEvent(override val id: UUID) : RoomEvent, EntityCreatedEvent<UUID>
