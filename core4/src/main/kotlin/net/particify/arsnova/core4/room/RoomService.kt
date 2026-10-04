/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.room

import java.util.UUID

interface RoomService {
  /** @throws net.particify.arsnova.core4.room.exception.RoomNotFoundException */
  fun getRoomById(id: UUID): Room
}
