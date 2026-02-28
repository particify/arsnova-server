/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user.internal

import jakarta.persistence.EntityManager
import java.time.Instant
import kotlin.collections.forEach
import net.particify.arsnova.core4.user.User
import net.particify.arsnova.core4.user.event.UserDeletedEvent
import net.particify.arsnova.core4.user.event.UsersMarkedForDeletionEvent
import net.particify.arsnova.core4.user.event.UsersMarkedForDeletionEvent.Kind
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.Limit
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.ScrollPosition
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private const val DELETE_BATCH_COUNT = 100
private const val DELETE_BATCH_SIZE = 10
private const val DELETE_MAX_TOTAL_SIZE = DELETE_BATCH_COUNT * DELETE_BATCH_SIZE

@Service
class UserBulkDeletionService(
    private val userService: UserServiceImpl,
    private val userProperties: UserProperties,
    private val eventPublisher: ApplicationEventPublisher,
    private val entityManager: EntityManager
) {
  private val logger = LoggerFactory.getLogger(UserServiceImpl::class.java)

  @Transactional
  fun deleteMarkedUsers() {
    logger.debug("Performing scheduled deletion of previously marked users...")
    val deletedAtBefore = Instant.now().minus(userProperties.deleteDelay)
    var totalCount = 0
    for (i in 0..<DELETE_BATCH_COUNT) {
      val position = ScrollPosition.offset(i.toLong())
      val window =
          userService.findByDeletedAtBefore(deletedAtBefore, position, Limit.of(DELETE_BATCH_SIZE))
      totalCount += window.size()
      window.forEach {
        eventPublisher.publishEvent(UserDeletedEvent(it.id!!))
        it.clearForSoftDelete()
        it.enabled = false
        it.roles.clear()
        userService.saveAndFlush(it)
        userService.delete(it)
      }
      entityManager.flush()
      entityManager.clear()
      if (!window.hasNext()) {
        break
      }
    }
    if (totalCount > 0) {
      logger.info("Deleted {} previously marked users.", totalCount)
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
