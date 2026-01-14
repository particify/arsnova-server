/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.room.event

import java.util.UUID
import org.jmolecules.event.annotation.Externalized

const val ROOM_CREATED_DESTINATION = "backend.event.room.aftercreation"

@Externalized(target = ROOM_CREATED_DESTINATION) data class RoomCreatedEvent(val id: UUID)
