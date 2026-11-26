/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user.internal

import java.util.Optional
import java.util.UUID
import net.particify.arsnova.core4.user.userId
import org.springframework.data.domain.AuditorAware
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

@Component("entityAuditor")
class EntityAuditor : AuditorAware<UUID> {
  override fun getCurrentAuditor(): Optional<UUID> {
    val authentication: Authentication? = SecurityContextHolder.getContext().authentication
    if (authentication == null || !authentication.isAuthenticated) {
      return Optional.empty()
    }
    return Optional.ofNullable(authentication.userId())
  }
}
