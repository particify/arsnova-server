/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.qna.internal

import java.util.UUID
import kotlin.reflect.KClass
import net.particify.arsnova.core4.qna.Qna
import net.particify.arsnova.core4.qna.QnaState
import net.particify.arsnova.core4.qna.exception.QnaNotFoundException
import net.particify.arsnova.core4.room.MembershipService
import net.particify.arsnova.core4.room.RoomRole
import net.particify.arsnova.core4.system.DomainPermissionEvaluation
import net.particify.arsnova.core4.system.DomainPermissionEvaluator
import net.particify.arsnova.core4.user.User
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
class QnaPermissionEvaluator(
    private val qnaRepository: QnaRepository,
    private val membershipService: MembershipService
) : DomainPermissionEvaluator<Qna> {
  private val logger = LoggerFactory.getLogger(this::class.java)

  override fun hasPermission(
      user: User,
      targetDomainObject: Qna,
      permission: Any
  ): DomainPermissionEvaluation {
    logger.debug("hasPermission({}, {}, {})", user, targetDomainObject, permission)
    val permission = if (permission == "delete") "write" else permission
    return DomainPermissionEvaluation(
        when (permission) {
          "write" ->
              membershipService
                  .findOneByRoomIdAndUserId(targetDomainObject.roomId!!, user.id!!)
                  ?.role !== RoomRole.PARTICIPANT
          "create_post" ->
              targetDomainObject.state === QnaState.STARTED ||
                  membershipService
                      .findOneByRoomIdAndUserId(targetDomainObject.roomId!!, user.id!!)
                      ?.role !== RoomRole.PARTICIPANT
          "read" ->
              targetDomainObject.state !== QnaState.STOPPED ||
                  membershipService
                      .findOneByRoomIdAndUserId(targetDomainObject.roomId!!, user.id!!)
                      ?.role !== RoomRole.PARTICIPANT
          else -> false
        },
        DomainPermissionEvaluation.PermissionReference(
            "Room", targetDomainObject.roomId!!, permission))
  }

  override fun findOneByKey(key: Any): Qna {
    check(key is UUID) { "Excepted key to be of type UUID." }
    return qnaRepository.findByIdOrNull(key) ?: throw QnaNotFoundException(key)
  }

  override fun supports(clazz: KClass<out Any>) = clazz == Qna::class

  override fun supports(className: String) = className == "Qna"
}
