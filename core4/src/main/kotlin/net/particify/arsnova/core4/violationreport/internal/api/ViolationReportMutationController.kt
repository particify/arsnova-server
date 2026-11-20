/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.violationreport.internal.api

import java.util.UUID
import net.particify.arsnova.core4.violationreport.internal.ViolationReportServiceImpl
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

@Controller
@PreAuthorize("hasRole('USER')")
@SchemaMapping(typeName = "Mutation")
class ViolationReportMutationController(
    private val violationReportService: ViolationReportServiceImpl
) {
  /** Returns only the id, so reporters cannot read the decision back. */
  @MutationMapping
  @PreAuthorize(
      "hasRole('VERIFIED') and " +
          "hasPermission(#input.targetId, " +
          "@violationReportTargetRegistry.permissionTargetType(#input.targetType), 'read')")
  fun createViolationReport(@Argument input: CreateViolationReportInput): UUID {
    return violationReportService.create(input.toViolationReport()).id!!
  }
}
