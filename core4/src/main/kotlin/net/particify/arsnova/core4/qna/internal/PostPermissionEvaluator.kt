/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.qna.internal

import java.util.UUID
import kotlin.reflect.KClass
import net.particify.arsnova.core4.qna.Post
import net.particify.arsnova.core4.qna.QnaState
import net.particify.arsnova.core4.qna.exception.PostNotFoundException
import net.particify.arsnova.core4.system.DomainPermissionEvaluation
import net.particify.arsnova.core4.system.DomainPermissionEvaluator
import net.particify.arsnova.core4.user.User
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
class PostPermissionEvaluator(private val postRepository: PostRepository) :
    DomainPermissionEvaluator<Post> {
  private val logger = LoggerFactory.getLogger(this::class.java)

  override fun hasPermission(
      user: User,
      targetDomainObject: Post,
      permission: Any
  ): DomainPermissionEvaluation {
    logger.debug("hasPermission({}, {}, {})", user, targetDomainObject, permission)
    val parentPermission =
        when (permission) {
          // Post permission -> Qna permission
          "write" -> "write"
          "read" -> if (targetDomainObject.qna!!.state !== QnaState.STOPPED) "read" else "write"
          "delete" -> "write"
          else -> return DomainPermissionEvaluation(false)
        }
    return DomainPermissionEvaluation(
        null,
        DomainPermissionEvaluation.PermissionReference(
            "Qna", targetDomainObject.qna!!.id!!, parentPermission))
  }

  override fun findOneByKey(key: Any): Post {
    check(key is UUID) { "Excepted key to be of type UUID." }
    return postRepository.findByIdOrNull(key) ?: throw PostNotFoundException(key)
  }

  override fun supports(clazz: KClass<out Any>) = clazz == Post::class

  override fun supports(className: String) = className == "Post"
}
