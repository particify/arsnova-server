/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.room.exception

import java.util.UUID

class RoomNotFoundException(val id: UUID?) : RuntimeException("Room not found") {
  constructor() : this(null)
}
