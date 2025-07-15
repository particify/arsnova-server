/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user.internal

import java.util.Optional
import java.util.UUID
import net.particify.arsnova.core4.user.User
import org.springframework.data.domain.AuditorAware
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

@Component
class EntityAuditor : AuditorAware<UUID> {
  override fun getCurrentAuditor(): Optional<UUID> {
    val authentication: Authentication? = SecurityContextHolder.getContext().authentication
    if (authentication == null ||
        !authentication.isAuthenticated ||
        authentication is AnonymousAuthenticationToken) {
      return Optional.empty()
    }
    return Optional.of((authentication.principal as User).id!!)
  }
}
