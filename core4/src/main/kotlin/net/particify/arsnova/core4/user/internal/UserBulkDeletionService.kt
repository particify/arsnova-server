/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user.internal

import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.collections.forEach
import net.particify.arsnova.core4.user.User
import net.particify.arsnova.core4.user.UserDeletedEvent
import net.particify.arsnova.core4.user.event.UsersMarkedForDeletionEvent
import net.particify.arsnova.core4.user.event.UsersMarkedForDeletionEvent.Kind
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.Limit
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.ScrollPosition
import org.springframework.data.support.WindowIterator
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private const val DELETE_AFTER_DAYS = 7L
private const val DELETE_BATCH_SIZE = 10
private const val DELETE_MAX_TOTAL_SIZE = 1000

@Service
class UserBulkDeletionService(
    private val userService: UserServiceImpl,
    private val userProperties: UserProperties,
    private val eventPublisher: ApplicationEventPublisher
) {
  private val logger = LoggerFactory.getLogger(UserServiceImpl::class.java)

  @Transactional
  fun deleteMarkedUsers() {
    val users =
        WindowIterator.of {
              userService.findByDeletedAtBefore(
                  Instant.now().minus(DELETE_AFTER_DAYS, ChronoUnit.DAYS),
                  it,
                  Limit.of(DELETE_BATCH_SIZE))
            }
            .startingAt(ScrollPosition.offset())
    users.forEachRemaining {
      eventPublisher.publishEvent(UserDeletedEvent(it.id!!))
      it.clearForSoftDelete()
      it.enabled = false
      it.roles.clear()
      userService.saveAndFlush(it)
      userService.delete(it)
    }
  }

  @Transactional
  fun deleteInactiveUnverifiedUsers() {
    val duration = userProperties.inactivityThresholds.unverified
    if (duration != null) {
      logger.debug("Marking inactive unverified users for deletion...")
      val before = Instant.now().minus(duration)
      deleteInactive(
          Kind.INACTIVE_UNVERIFIED,
          before,
          userService::findByUsernameIsNullAndDeletedAtIsNullAndLastActivityAtBefore)
    }
  }

  @Transactional
  fun deleteInactiveSingleVisitUnverifiedUsers() {
    val duration = userProperties.inactivityThresholds.unverifiedSingleVisit
    if (duration != null) {
      logger.debug("Marking inactive, unverified users (single visit) for deletion...")
      val before = Instant.now().minus(duration)
      deleteInactive(
          Kind.INACTIVE_UNVERIFIED,
          before,
          userService::findSingleVisitUnverifiedUsersCreatedAtLessThan)
    }
  }

  @Transactional
  fun deleteInactiveVerifiedUsers() {
    val duration = userProperties.inactivityThresholds.verified
    if (duration != null) {
      logger.debug("Marking inactive, verified users for deletion...")
      val before = Instant.now().minus(duration)
      deleteInactive(
          Kind.INACTIVE_VERIFIED,
          before,
          userService::findByUsernameNotNullAndDeletedAtIsNullAndLastActivityAtBefore)
    }
  }

  private fun deleteInactive(
      kind: Kind,
      before: Instant,
      fn: (Instant, PageRequest) -> List<User>
  ) {
    var pageNumber = 0
    var total = 0
    do {
      val users = fn(before, PageRequest.of(pageNumber, DELETE_BATCH_SIZE))
      users.forEach(userService::markAccountForDeletion)
      userService.flush()
      total += users.size
      pageNumber++
    } while (users.isNotEmpty() && total < DELETE_MAX_TOTAL_SIZE)
    if (total > 0) {
      logger.info("Marked {} inactive users ({}) for deletion.", total, kind)
      eventPublisher.publishEvent(UsersMarkedForDeletionEvent(kind, total))
    }
  }
}
