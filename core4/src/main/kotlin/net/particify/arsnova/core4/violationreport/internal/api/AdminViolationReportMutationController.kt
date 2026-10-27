/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.violationreport.internal.api

import java.util.UUID
import net.particify.arsnova.core4.violationreport.ViolationReport
import net.particify.arsnova.core4.violationreport.event.ViolationReportDecision
import net.particify.arsnova.core4.violationreport.internal.ViolationReportServiceImpl
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

@Controller
@PreAuthorize("hasRole('ADMIN')")
@SchemaMapping(typeName = "Mutation")
class AdminViolationReportMutationController(
    private val violationReportService: ViolationReportServiceImpl
) {
  @MutationMapping
  fun adminDecideViolationReport(
      @Argument id: UUID,
      @Argument decision: ViolationReportDecision
  ): ViolationReport {
    return violationReportService.decide(id, decision)
  }
}
