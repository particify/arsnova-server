/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.violationreport.internal.api

import java.util.UUID
import net.particify.arsnova.core4.violationreport.ViolationReport
import net.particify.arsnova.core4.violationreport.ViolationReportReason

data class CreateViolationReportInput(
    val targetType: String,
    val targetId: UUID,
    val reason: ViolationReportReason,
    val description: String
) {
  fun toViolationReport() =
      ViolationReport(
          targetType = targetType, targetId = targetId, reason = reason, description = description)
}
