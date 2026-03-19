/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.qna.internal

import java.util.UUID
import kotlin.reflect.KClass
import net.particify.arsnova.core4.qna.Post
import net.particify.arsnova.core4.qna.QnaState
import net.particify.arsnova.core4.qna.exception.PostNotFoundException
import net.particify.arsnova.core4.room.MembershipService
import net.particify.arsnova.core4.room.RoomRole
import net.particify.arsnova.core4.system.DomainPermissionEvaluation
import net.particify.arsnova.core4.system.DomainPermissionEvaluator
import net.particify.arsnova.core4.user.User
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
class PostPermissionEvaluator(
    private val postRepository: PostRepository,
    private val membershipService: MembershipService
) : DomainPermissionEvaluator<Post> {
  private val logger = LoggerFactory.getLogger(this::class.java)

  override fun hasPermission(
      user: User,
      targetDomainObject: Post,
      permission: Any
  ): DomainPermissionEvaluation {
    logger.debug("hasPermission({}, {}, {})", user, targetDomainObject, permission)
    val permission = if (permission == "delete") "write" else permission
    return DomainPermissionEvaluation(
        when (permission) {
          "write" ->
              membershipService
                  .findOneByRoomIdAndUserId(targetDomainObject.qna!!.roomId!!, user.id!!)
                  ?.role !== RoomRole.PARTICIPANT
          "read" ->
              targetDomainObject.qna!!.state !== QnaState.STOPPED ||
                  membershipService
                      .findOneByRoomIdAndUserId(targetDomainObject.qna!!.roomId!!, user.id!!)
                      ?.role !== RoomRole.PARTICIPANT
          else -> false
        },
        DomainPermissionEvaluation.PermissionReference(
            "Qna", targetDomainObject.qna!!.id!!, permission))
  }

  override fun findOneByKey(key: Any): Post {
    check(key is UUID) { "Excepted key to be of type UUID." }
    return postRepository.findByIdOrNull(key) ?: throw PostNotFoundException(key)
  }

  override fun supports(clazz: KClass<out Any>) = clazz == Post::class

  override fun supports(className: String) = className == "Post"
}
