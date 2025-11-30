/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system

import kotlin.reflect.KClass
import net.particify.arsnova.core4.user.User

/** Implementations evaluate permissions for a specific domain type [T]. */
interface DomainPermissionEvaluator<T : Any> {
  fun hasPermission(user: User, targetDomainObject: T, permission: Any): DomainPermissionEvaluation

  fun findOneByKey(key: Any): T

  fun supports(clazz: KClass<out Any>): Boolean

  fun supports(className: String): Boolean
}
