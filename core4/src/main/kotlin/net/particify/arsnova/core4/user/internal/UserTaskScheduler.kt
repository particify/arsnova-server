/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user.internal

import java.util.concurrent.TimeUnit
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class UserTaskScheduler(private val userService: UserServiceImpl) {
  @Scheduled(initialDelay = 60, fixedDelay = 15, timeUnit = TimeUnit.MINUTES)
  fun deleteMarkedUsers() {
    userService.deleteMarkedUsers()
  }
}
