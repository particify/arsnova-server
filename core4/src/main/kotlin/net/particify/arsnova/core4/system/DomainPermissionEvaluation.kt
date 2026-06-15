/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system

import java.util.UUID

/**
 * Holds either the result of a permission check or a reference to another permission check which
 * should be performed instead.
 */
data class DomainPermissionEvaluation(
    val hasPermission: Boolean?,
    val permissionReference: PermissionReference? = null
) {
  data class PermissionReference(val targetClassName: String, val id: UUID, val permission: Any)

  init {
    require(
        (hasPermission == null && permissionReference != null) ||
            (hasPermission != null && permissionReference == null)) {
          "Only one, either hasPermission or permissionReference, may be defined."
        }
  }
}
