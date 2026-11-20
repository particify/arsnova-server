/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.violationreport.internal.api

import net.particify.arsnova.core4.violationreport.AdminViolationReportStats
import net.particify.arsnova.core4.violationreport.internal.ViolationReportServiceImpl
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

@Controller
@PreAuthorize("hasRole('ADMIN')")
@SchemaMapping(typeName = "Query")
class AdminViolationReportStatisticsQueryController(
    private val violationReportService: ViolationReportServiceImpl,
) {

  @QueryMapping
  fun adminViolationReportStats(): AdminViolationReportStats {
    return AdminViolationReportStats(
        totalCount = violationReportService.count(),
        undecidedCount = violationReportService.countByDecisionIsNull(),
        countsByReason = violationReportService.countGroupedByReason(),
        countsByDecision = violationReportService.countGroupedByDecision(),
    )
  }
}
