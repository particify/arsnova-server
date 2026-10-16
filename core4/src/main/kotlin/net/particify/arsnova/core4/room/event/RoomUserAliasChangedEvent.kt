/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.room.event

import java.util.UUID
import org.jmolecules.event.annotation.DomainEvent

/** Signals that a user's alias for a room was set, so views showing it can refresh. */
@DomainEvent data class RoomUserAliasChangedEvent(val roomId: UUID, val userId: UUID) : RoomEvent
