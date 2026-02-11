/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.room.event

import java.util.UUID
import org.jmolecules.event.annotation.DomainEvent

@DomainEvent class DemoRoomDuplicatedEvent(val id: UUID) : RoomEvent
