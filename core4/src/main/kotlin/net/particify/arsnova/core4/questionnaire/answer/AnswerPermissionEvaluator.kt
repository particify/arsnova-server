/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.answer

import java.util.UUID
import kotlin.reflect.KClass
import net.particify.arsnova.core4.questionnaire.exception.AnswerNotFoundException
import net.particify.arsnova.core4.security.DomainPermissionEvaluation
import net.particify.arsnova.core4.security.DomainPermissionEvaluator
import net.particify.arsnova.core4.user.User
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
class AnswerPermissionEvaluator(
    private val answerRepository: AnswerRepository,
) : DomainPermissionEvaluator<Answer> {
  private val logger = LoggerFactory.getLogger(this::class.java)

  override fun hasPermission(
      user: User,
      targetDomainObject: Answer,
      permission: Any
  ): DomainPermissionEvaluation {
    logger.debug("hasPermission({}, {}, {})", user, targetDomainObject, permission)
    val reference =
        when (permission) {
          "write" -> roundReference(targetDomainObject, "write")
          "read" -> roundReference(targetDomainObject, "read")
          "delete" -> roundReference(targetDomainObject, "write")
          "moderate" -> moderationReference(targetDomainObject)
          else -> null
        } ?: return DomainPermissionEvaluation(false)
    return DomainPermissionEvaluation(null, reference)
  }

  private fun roundReference(answer: Answer, permission: String) =
      DomainPermissionEvaluation.PermissionReference("Round", answer.round!!.id!!, permission)

  /**
   * Moderating answers is a room-wide right which moderators hold without being able to edit the
   * questionnaire, so it is checked against the room rather than through the content chain.
   */
  private fun moderationReference(answer: Answer) =
      answer.round?.content?.questionnaire?.roomId?.let {
        DomainPermissionEvaluation.PermissionReference("Room", it, "moderate")
      }

  override fun findOneByKey(key: Any): Answer {
    check(key is UUID) { "Excepted key to be of type UUID." }
    return answerRepository.findByIdOrNull(key) ?: throw AnswerNotFoundException(key)
  }

  override fun supports(clazz: KClass<out Any>) = clazz == Answer::class

  override fun supports(className: String) = className == "Answer"
}
