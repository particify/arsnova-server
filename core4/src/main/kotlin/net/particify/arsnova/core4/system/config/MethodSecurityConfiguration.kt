/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.config

import net.particify.arsnova.core4.system.security.DelegatingPermissionEvaluator
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity

@Configuration
@EnableMethodSecurity(prePostEnabled = true)
class MethodSecurityConfiguration {
  @Bean
  fun expressionHandler(
      permissionEvaluator: DelegatingPermissionEvaluator
  ): MethodSecurityExpressionHandler {
    val handler = DefaultMethodSecurityExpressionHandler()
    handler.setPermissionEvaluator(permissionEvaluator)
    return handler
  }
}
