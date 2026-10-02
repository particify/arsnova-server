/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.security.config

import java.io.Serializable
import net.particify.arsnova.core4.security.internal.DelegatingPermissionEvaluator
import org.springframework.beans.factory.ObjectProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.access.PermissionEvaluator
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.core.Authentication

@Configuration
@EnableMethodSecurity(prePostEnabled = true)
class MethodSecurityConfiguration {
  @Bean
  fun expressionHandler(
      permissionEvaluator: ObjectProvider<DelegatingPermissionEvaluator>
  ): MethodSecurityExpressionHandler {
    val handler = DefaultMethodSecurityExpressionHandler()
    handler.setPermissionEvaluator(DeferredPermissionEvaluator(permissionEvaluator))
    return handler
  }

  /**
   * Building the method security advisor would otherwise build every domain evaluator and the
   * persistence beans behind them, which AOT processing cannot do.
   */
  private class DeferredPermissionEvaluator(
      private val delegate: ObjectProvider<out PermissionEvaluator>
  ) : PermissionEvaluator {
    override fun hasPermission(
        authentication: Authentication,
        targetDomainObject: Any?,
        permission: Any
    ) = delegate.getObject().hasPermission(authentication, targetDomainObject, permission)

    override fun hasPermission(
        authentication: Authentication,
        targetId: Serializable,
        targetType: String,
        permission: Any
    ) = delegate.getObject().hasPermission(authentication, targetId, targetType, permission)
  }
}
