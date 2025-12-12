/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.announcement.internal.api

import net.particify.arsnova.core4.announcement.AdminAnnouncementStats
import net.particify.arsnova.core4.announcement.internal.AnnouncementServiceImpl
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

@Controller
@PreAuthorize("hasRole('ADMIN')")
@SchemaMapping(typeName = "Query")
class AdminAnnouncementStatisticsQueryController(
    private val announcementService: AnnouncementServiceImpl,
) {

  @QueryMapping
  fun adminAnnouncementStats(): AdminAnnouncementStats {
    return AdminAnnouncementStats(
        totalCount = announcementService.count(),
    )
  }
}
