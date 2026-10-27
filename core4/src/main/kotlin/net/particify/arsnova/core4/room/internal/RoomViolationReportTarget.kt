/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.room.internal

import net.particify.arsnova.core4.violationreport.event.ViolationReportTarget
import org.springframework.stereotype.Component

@Component
class RoomViolationReportTarget : ViolationReportTarget {
  companion object {
    const val TYPE = "ROOM"
  }

  override val type = TYPE
  override val permissionTargetType = "Room"
}
