/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.room.exception

import net.particify.arsnova.core4.room.Membership

class MembershipNotFoundException(val id: Membership.RoomUserId?) : RuntimeException() {
  constructor() : this(null)
}
