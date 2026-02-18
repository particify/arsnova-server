/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user.internal

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class UserTaskScheduler(private val userBulkDeletionService: UserBulkDeletionService) {
  private val logger = LoggerFactory.getLogger(UserTaskScheduler::class.java)

  @Scheduled(cron = $$"${tasks.schedule.marked-user-deletion}", zone = $$"${tasks.time-zone}")
  fun deleteMarkedUsers() {
    logger.trace("Running scheduled deleteMarkedUsers task...")
    userBulkDeletionService.deleteMarkedUsers()
  }

  @Scheduled(cron = $$"${tasks.schedule.inactive-user-deletion}", zone = $$"${tasks.time-zone}")
  fun deleteInactiveUnverifiedUsers() {
    logger.trace("Running scheduled deleteInactiveUnverifiedUsers task...")
    userBulkDeletionService.deleteInactiveUnverifiedUsers()
    userBulkDeletionService.deleteInactiveSingleVisitUnverifiedUsers()
    userBulkDeletionService.deleteInactiveVerifiedUsers()
  }
}
