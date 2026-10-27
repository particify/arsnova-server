/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.violationreport.event

/**
 * Registers a type of entity as a target of violation reports. The module owning the entity
 * provides it as a bean and adds [type] to the `ViolationReportTargetType` GraphQL enum.
 *
 * Register a target only once both its permission evaluator and a listener deleting the target on
 * [ViolationReportDecision.REMOVAL] exist: a [permissionTargetType] no evaluator supports fails
 * every report creation with an internal error.
 */
interface ViolationReportTarget {
  /** The value stored with a report and published in [ViolationReportDecidedEvent.targetType]. */
  val type: String

  /** The target type name the permission evaluator of the entity supports. */
  val permissionTargetType: String
}
