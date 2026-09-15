/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.qna.internal

import java.util.UUID
import kotlin.reflect.KClass
import net.particify.arsnova.core4.qna.Reply
import net.particify.arsnova.core4.qna.exception.ReplyNotFoundException
import net.particify.arsnova.core4.system.DomainPermissionEvaluation
import net.particify.arsnova.core4.system.DomainPermissionEvaluator
import net.particify.arsnova.core4.user.User
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
class ReplyPermissionEvaluator(private val replyRepository: ReplyRepository) :
    DomainPermissionEvaluator<Reply> {
  private val logger = LoggerFactory.getLogger(this::class.java)

  override fun hasPermission(
      user: User,
      targetDomainObject: Reply,
      permission: Any
  ): DomainPermissionEvaluation {
    logger.debug("hasPermission({}, {}, {})", user, targetDomainObject, permission)
    return DomainPermissionEvaluation(
        null,
        DomainPermissionEvaluation.PermissionReference(
            "Post", targetDomainObject.post!!.id!!, permission))
  }

  override fun findOneByKey(key: Any): Reply {
    check(key is UUID) { "Excepted key to be of type UUID." }
    return replyRepository.findByIdOrNull(key) ?: throw ReplyNotFoundException(key)
  }

  override fun supports(clazz: KClass<out Any>) = clazz == Reply::class

  override fun supports(className: String) = className == "Reply"
}
