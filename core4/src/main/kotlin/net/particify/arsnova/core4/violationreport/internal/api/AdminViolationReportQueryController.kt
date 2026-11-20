/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.violationreport.internal.api

import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import net.particify.arsnova.core4.violationreport.QViolationReport
import net.particify.arsnova.core4.violationreport.ViolationReport
import net.particify.arsnova.core4.violationreport.internal.ViolationReportServiceImpl
import org.springframework.data.domain.ScrollPosition
import org.springframework.data.domain.Sort
import org.springframework.data.domain.Window
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.graphql.data.query.ScrollSubrange
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

@Controller
@PreAuthorize("hasRole('ADMIN')")
@SchemaMapping(typeName = "Query")
class AdminViolationReportQueryController(
    private val violationReportService: ViolationReportServiceImpl
) {
  companion object {
    const val DEFAULT_QUERY_LIMIT = 50
  }

  @QueryMapping
  fun adminViolationReportsByDecided(
      @Argument decided: Boolean,
      subrange: ScrollSubrange
  ): Window<ViolationReport> {
    val decision = QViolationReport.violationReport.decision
    val predicate = if (decided) decision.isNotNull else decision.isNull
    return violationReportService.findBy(predicate) { q ->
      q.sortBy(Sort.by("id").descending())
          .limit(DEFAULT_QUERY_LIMIT)
          .scroll(subrange.position().orElse(ScrollPosition.offset()))
    }
  }

  @SchemaMapping(typeName = "ViolationReport")
  fun createdBy(report: ViolationReport): UUID? {
    return report.auditMetadata.createdBy
  }

  @SchemaMapping(typeName = "ViolationReport")
  fun createdAt(report: ViolationReport): OffsetDateTime? {
    return report.auditMetadata.createdAt?.atOffset(ZoneOffset.UTC)
  }

  @SchemaMapping(typeName = "ViolationReport")
  fun updatedBy(report: ViolationReport): UUID? {
    return report.auditMetadata.updatedBy
  }

  @SchemaMapping(typeName = "ViolationReport")
  fun updatedAt(report: ViolationReport): OffsetDateTime? {
    return report.auditMetadata.updatedAt?.atOffset(ZoneOffset.UTC)
  }
}
