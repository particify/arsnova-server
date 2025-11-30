/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.room

import java.util.UUID
import kotlin.reflect.KClass
import net.particify.arsnova.core4.room.exception.RoomNotFoundException
import net.particify.arsnova.core4.room.internal.MembershipRepository
import net.particify.arsnova.core4.room.internal.RoomRepository
import net.particify.arsnova.core4.system.DomainPermissionEvaluation
import net.particify.arsnova.core4.system.DomainPermissionEvaluator
import net.particify.arsnova.core4.user.User
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
class RoomPermissionEvaluator(
    val membershipRepository: MembershipRepository,
    val roomRepository: RoomRepository
) : DomainPermissionEvaluator<Room> {
  private val logger = LoggerFactory.getLogger(this::class.java)

  override fun hasPermission(
      user: User,
      targetDomainObject: Room,
      permission: Any
  ): DomainPermissionEvaluation {
    logger.debug("hasPermission({}, {}, {})", user, targetDomainObject, permission)
    val membership =
        membershipRepository.findOneByRoomIdAndUserId(targetDomainObject.id!!, user.id!!)
    return DomainPermissionEvaluation(
        when (membership?.role) {
          RoomRole.OWNER -> true
          RoomRole.EDITOR ->
              when (permission) {
                "read" -> true
                "write" -> true
                else -> false
              }
          RoomRole.MODERATOR,
          RoomRole.PARTICIPANT ->
              when (permission) {
                "read" -> true
                else -> false
              }
          else -> false
        })
  }

  override fun findOneByKey(key: Any): Room =
      when (key) {
        is UUID -> roomRepository.findByIdOrNull(key) ?: throw RoomNotFoundException(key)
        is Int -> roomRepository.findOneByShortId(key) ?: throw RoomNotFoundException()
        is String -> roomRepository.findOneByShortId(key.toInt()) ?: throw RoomNotFoundException()
        else -> error("Unsupported type for key")
      }

  override fun supports(clazz: KClass<out Any>) = clazz == Room::class

  override fun supports(className: String) = className == "Room"
}
