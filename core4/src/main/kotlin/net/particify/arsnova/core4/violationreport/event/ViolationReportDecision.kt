/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.violationreport.event

enum class ViolationReportDecision {
  INVALID,

  /** The module owning the target deletes it. */
  REMOVAL
}
