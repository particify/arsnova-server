/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.violationreport.internal

import net.particify.arsnova.core4.common.exception.InvalidInputException
import net.particify.arsnova.core4.violationreport.event.ViolationReportTarget
import org.springframework.stereotype.Component

@Component
class ViolationReportTargetRegistry(targets: List<ViolationReportTarget>) {
  private val permissionTargetTypes = targets.associate { it.type to it.permissionTargetType }

  init {
    require(permissionTargetTypes.size == targets.size) {
      "Duplicate violation report target types: ${targets.map { it.type }}"
    }
  }

  /**
   * Referenced from the `@PreAuthorize` expression of report creation, so an unregistered type is
   * rejected as invalid input before any permission evaluation.
   */
  fun permissionTargetType(type: String): String =
      permissionTargetTypes[type]
          ?: throw InvalidInputException("Unknown violation report target type: $type")
}
