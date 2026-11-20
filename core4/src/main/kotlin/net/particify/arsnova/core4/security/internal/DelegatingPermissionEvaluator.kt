/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.security.internal

import java.io.Serializable
import kotlin.Any
import net.particify.arsnova.core4.security.DomainPermissionEvaluator
import net.particify.arsnova.core4.user.User
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.ObjectProvider
import org.springframework.security.access.PermissionEvaluator
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component

/**
 * This PermissionEvaluator delegates the actual evaluation to the appropriate
 * DomainPermissionEvaluator based on the target type.
 */
@Component
class DelegatingPermissionEvaluator(
    // Method security creates this evaluator while building its advisor, so the domain evaluators
    // and the persistence beans behind them are only resolved on first use. Unlike @Lazy, an
    // ObjectProvider needs no proxy, so this does not depend on proxy support in a native image.
    permissionEvaluatorProvider: ObjectProvider<DomainPermissionEvaluator<*>>
) : PermissionEvaluator {
  private val logger = LoggerFactory.getLogger(this::class.java)
  val permissionEvaluators: List<DomainPermissionEvaluator<Any>> by lazy {
    val permissionEvaluators = permissionEvaluatorProvider.orderedStream().toList()
    logger.debug("Registered DomainPermissionEvaluators: {}", permissionEvaluators)
    permissionEvaluators.map { it as DomainPermissionEvaluator<Any> }
  }

  override fun hasPermission(
      authentication: Authentication,
      targetDomainObject: Any?,
      permission: Any
  ): Boolean {
    logger.debug("hasPermission({}, {}, {})", authentication, targetDomainObject, permission)
    requireNotNull(targetDomainObject)
    val user = authentication.principal as? User ?: return false
    val evaluator =
        permissionEvaluators.find { it.supports(targetDomainObject::class) }
            ?: error("Unsupported type '${targetDomainObject::class.simpleName}'")
    logger.trace("Delegating to {}", evaluator::class.simpleName)
    val evaluation = evaluator.hasPermission(user, targetDomainObject, permission)
    val parentPermission = evaluation.permissionReference
    return evaluation.hasPermission
        ?: hasPermission(
            authentication,
            parentPermission!!.id,
            parentPermission.targetClassName,
            parentPermission.permission)
  }

  override fun hasPermission(
      authentication: Authentication,
      targetId: Serializable,
      targetType: String,
      permission: Any
  ): Boolean {
    logger.debug("hasPermission({}, {}, {}, {})", authentication, targetId, targetType, permission)
    if (authentication.principal !is User) {
      return false
    }
    val evaluator =
        permissionEvaluators.find { it.supports(targetType) }
            ?: error("Unsupported type '$targetType'")
    val targetDomainObject = evaluator.findOneByKey(targetId)
    return hasPermission(authentication, targetDomainObject, permission)
  }
}
