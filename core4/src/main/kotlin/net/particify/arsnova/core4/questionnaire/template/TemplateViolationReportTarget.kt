/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.template

import net.particify.arsnova.core4.violationreport.event.ViolationReportTarget
import org.springframework.stereotype.Component

@Component
class TemplateViolationReportTarget : ViolationReportTarget {
  companion object {
    const val TYPE = "TEMPLATE"
  }

  override val type = TYPE
  override val permissionTargetType = "Template"
}
