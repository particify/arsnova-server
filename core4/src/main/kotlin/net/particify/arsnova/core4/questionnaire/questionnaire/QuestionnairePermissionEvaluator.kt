/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.questionnaire

import java.util.UUID
import kotlin.reflect.KClass
import net.particify.arsnova.core4.questionnaire.exception.QuestionnaireNotFoundException
import net.particify.arsnova.core4.security.DomainPermissionEvaluation
import net.particify.arsnova.core4.security.DomainPermissionEvaluator
import net.particify.arsnova.core4.user.User
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
class QuestionnairePermissionEvaluator(
    private val questionnaireRepository: QuestionnaireRepository,
) : DomainPermissionEvaluator<Questionnaire> {
  private val logger = LoggerFactory.getLogger(this::class.java)

  override fun hasPermission(
      user: User,
      targetDomainObject: Questionnaire,
      permission: Any
  ): DomainPermissionEvaluation {
    logger.debug("hasPermission({}, {}, {})", user, targetDomainObject, permission)
    val parentPermission =
        when (permission) {
          "write" -> "write"
          // Moderators may open what they can list, unpublished questionnaires included.
          "read" -> if (targetDomainObject.published) "read" else "moderate"
          "moderate" -> "moderate"
          // Participants only see the leaderboard of a published questionnaire which enables it,
          // while moderators see it throughout, as they do the statistics.
          "readLeaderboard" ->
              if (targetDomainObject.published && targetDomainObject.leaderboardEnabled == true)
                  "read"
              else "moderate"
          "delete" -> "write"
          else -> return DomainPermissionEvaluation(false)
        }
    return DomainPermissionEvaluation(
        null,
        DomainPermissionEvaluation.PermissionReference(
            "Room", targetDomainObject.roomId!!, parentPermission))
  }

  override fun findOneByKey(key: Any): Questionnaire {
    check(key is UUID) { "Excepted key to be of type UUID." }
    return questionnaireRepository.findByIdOrNull(key) ?: throw QuestionnaireNotFoundException(key)
  }

  override fun supports(clazz: KClass<out Any>) = clazz == Questionnaire::class

  override fun supports(className: String) = className == "Questionnaire"
}
