/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.common.event

import java.util.UUID
import kotlin.reflect.KClass

data class EntityChangeEvent<T : Any>(
    val entityType: KClass<T>,
    val changeType: ChangeType,
    val entityId: UUID
) {
  fun entityTypeString() = entityType.simpleName

  enum class ChangeType {
    CREATE,
    UPDATE,
    DELETE
  }
}
