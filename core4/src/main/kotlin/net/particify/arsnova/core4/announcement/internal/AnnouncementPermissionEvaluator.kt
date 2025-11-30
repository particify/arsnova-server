/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.announcement.internal

import java.util.UUID
import kotlin.reflect.KClass
import net.particify.arsnova.core4.announcement.Announcement
import net.particify.arsnova.core4.announcement.exception.AnnouncementNotFoundException
import net.particify.arsnova.core4.system.DomainPermissionEvaluation
import net.particify.arsnova.core4.system.DomainPermissionEvaluator
import net.particify.arsnova.core4.user.User
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
class AnnouncementPermissionEvaluator(val announcementRepository: AnnouncementRepository) :
    DomainPermissionEvaluator<Announcement> {
  private val logger = LoggerFactory.getLogger(this::class.java)

  override fun hasPermission(
      user: User,
      targetDomainObject: Announcement,
      permission: Any
  ): DomainPermissionEvaluation {
    logger.debug("hasPermission({}, {}, {})", user, targetDomainObject, permission)
    val permission = if (permission == "delete") "write" else permission
    return DomainPermissionEvaluation(
        null,
        DomainPermissionEvaluation.PermissionReference(
            "Room", targetDomainObject.room?.id!!, permission))
  }

  override fun findOneByKey(key: Any): Announcement {
    check(key is UUID) { "Excepted key to be of type UUID." }
    return announcementRepository.findByIdOrNull(key) ?: throw AnnouncementNotFoundException(key)
  }

  override fun supports(clazz: KClass<out Any>) = clazz == Announcement::class

  override fun supports(className: String) = className == "Announcement"
}
