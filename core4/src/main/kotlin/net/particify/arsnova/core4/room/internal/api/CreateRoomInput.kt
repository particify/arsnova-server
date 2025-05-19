/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.room.internal.api

import net.particify.arsnova.core4.room.Room

data class CreateRoomInput(val name: String) {
  fun toRoom(shortId: Int) = Room(shortId = shortId, name = name)
}
