/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.room

import java.util.Locale
import java.util.UUID

interface RoomUserAliasService {
  fun findOneByRoomIdAndUserId(roomId: UUID, userId: UUID): RoomUserAlias?

  /**
   * Returns the user's alias for the room or an unpersisted proposal if the user has not set one
   * yet. Aliases without an explicitly set name are generated from their seed for the given locale.
   */
  fun retrieveOrGenerate(roomId: UUID, userId: UUID, locale: Locale): RoomUserAlias

  /** Sets the user's alias for the room. Aliases cannot be changed once they have been set. */
  fun set(roomId: UUID, userId: UUID, alias: String?, seed: Int?, locale: Locale): RoomUserAlias
}
