/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.violationreport

import net.particify.arsnova.core4.violationreport.event.ViolationReportDecision

data class AdminViolationReportStats(
    val totalCount: Long = 0,
    val undecidedCount: Long = 0,
    val countsByReason: List<ViolationReportReasonCount> = emptyList(),
    val countsByDecision: List<ViolationReportDecisionCount> = emptyList(),
)

data class ViolationReportReasonCount(val reason: ViolationReportReason, val count: Long)

data class ViolationReportDecisionCount(val decision: ViolationReportDecision, val count: Long)
