/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user.internal.api

import net.particify.arsnova.core4.user.AdminUserStats
import net.particify.arsnova.core4.user.internal.UserServiceImpl
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

@Controller
@PreAuthorize("hasRole('ADMIN')")
@SchemaMapping(typeName = "Query")
class AdminUserStatisticsQueryController(
    private val userService: UserServiceImpl,
) {

  @QueryMapping
  fun adminUserStats(): AdminUserStats {
    return AdminUserStats(
        totalCount = userService.count(),
        verifiedCount = userService.countByUsernameIsNotNull(),
        pendingCount = userService.countByUsernameIsNullAndUnverifiedMailAddressIsNotNull())
  }
}
