/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.security

import kotlin.reflect.KClass
import net.particify.arsnova.core4.user.User
import org.springframework.modulith.NamedInterface

/** Implementations evaluate permissions for a specific domain type [T]. */
@NamedInterface("permission")
interface DomainPermissionEvaluator<T : Any> {
  fun hasPermission(user: User, targetDomainObject: T, permission: Any): DomainPermissionEvaluation

  fun findOneByKey(key: Any): T

  fun supports(clazz: KClass<out Any>): Boolean

  fun supports(className: String): Boolean
}
