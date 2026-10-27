/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.violationreport.internal

import java.util.UUID
import net.particify.arsnova.core4.violationreport.ViolationReport
import net.particify.arsnova.core4.violationreport.ViolationReportDecisionCount
import net.particify.arsnova.core4.violationreport.ViolationReportReasonCount
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.querydsl.QuerydslPredicateExecutor

interface ViolationReportRepository :
    JpaRepository<ViolationReport, UUID>, QuerydslPredicateExecutor<ViolationReport> {
  fun findByTargetTypeAndTargetIdAndDecisionIsNull(
      targetType: String,
      targetId: UUID
  ): List<ViolationReport>

  fun countByDecisionIsNull(): Long

  @Query(
      "SELECT new net.particify.arsnova.core4.violationreport.ViolationReportReasonCount(" +
          "r.reason, COUNT(r)) FROM ViolationReport r GROUP BY r.reason ORDER BY r.reason")
  fun countGroupedByReason(): List<ViolationReportReasonCount>

  @Query(
      "SELECT new net.particify.arsnova.core4.violationreport.ViolationReportDecisionCount(" +
          "r.decision, COUNT(r)) FROM ViolationReport r WHERE r.decision IS NOT NULL " +
          "GROUP BY r.decision ORDER BY r.decision")
  fun countGroupedByDecision(): List<ViolationReportDecisionCount>
}
