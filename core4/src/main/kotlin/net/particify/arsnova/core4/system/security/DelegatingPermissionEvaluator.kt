/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.security

import java.io.Serializable
import kotlin.Any
import net.particify.arsnova.core4.system.DomainPermissionEvaluator
import net.particify.arsnova.core4.user.User
import org.slf4j.LoggerFactory
import org.springframework.security.access.PermissionEvaluator
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component

/**
 * This PermissionEvaluator delegates the actual evaluation to the appropriate
 * DomainPermissionEvaluator based on the target type.
 */
@Component
class DelegatingPermissionEvaluator(
    permissionEvaluators: List<DomainPermissionEvaluator<out Any>>
) : PermissionEvaluator {
  private val logger = LoggerFactory.getLogger(this::class.java)
  val permissionEvaluators: List<DomainPermissionEvaluator<Any>> =
      permissionEvaluators.map { it as DomainPermissionEvaluator<Any> }

  init {
    logger.debug("Registered DomainPermissionEvaluators: {}", permissionEvaluators)
  }

  override fun hasPermission(
      authentication: Authentication,
      targetDomainObject: Any,
      permission: Any
  ): Boolean {
    logger.debug("hasPermission({}, {}, {})", authentication, targetDomainObject, permission)
    val user = authentication.principal as? User ?: error("Unexpected principal type")
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
    val evaluator =
        permissionEvaluators.find { it.supports(targetType) }
            ?: error("Unsupported type '$targetType'")
    val targetDomainObject = evaluator.findOneByKey(targetId)
    return hasPermission(authentication, targetDomainObject, permission)
  }
}
