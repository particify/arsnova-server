/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.questionnaire

import java.util.UUID
import kotlin.reflect.KClass
import net.particify.arsnova.core4.questionnaire.exception.QuestionnaireNotFoundException
import net.particify.arsnova.core4.questionnaire.template.TemplatePermissionEvaluator
import net.particify.arsnova.core4.security.DomainPermissionEvaluation
import net.particify.arsnova.core4.security.DomainPermissionEvaluator
import net.particify.arsnova.core4.user.User
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
class QuestionnairePermissionEvaluator(
    private val questionnaireRepository: QuestionnaireRepository,
    private val templatePermissionEvaluator: TemplatePermissionEvaluator,
) : DomainPermissionEvaluator<Questionnaire> {
  private val logger = LoggerFactory.getLogger(this::class.java)

  override fun hasPermission(
      user: User,
      targetDomainObject: Questionnaire,
      permission: Any
  ): DomainPermissionEvaluation {
    logger.debug("hasPermission({}, {}, {})", user, targetDomainObject, permission)
    val roomId = targetDomainObject.roomId
    return if (roomId == null) templatePermission(user, targetDomainObject, permission)
    else roomPermission(targetDomainObject, roomId, permission)
  }

  private fun roomPermission(
      questionnaire: Questionnaire,
      roomId: UUID,
      permission: Any
  ): DomainPermissionEvaluation {
    val parentPermission =
        when (permission) {
          "write" -> "write"
          // Moderators may open what they can list, unpublished questionnaires included.
          "read" -> if (questionnaire.published) "read" else "moderate"
          "moderate" -> "moderate"
          // Participants only see the leaderboard of a published questionnaire which enables it,
          // while moderators see it throughout, as they do the statistics.
          "readLeaderboard" ->
              if (questionnaire.published && questionnaire.leaderboardEnabled == true) "read"
              else "moderate"
          "delete" -> "write"
          else -> return DomainPermissionEvaluation(false)
        }
    return DomainPermissionEvaluation(
        null, DomainPermissionEvaluation.PermissionReference("Room", roomId, parentPermission))
  }

  /**
   * A room-less questionnaire holds a template's contents, so the template decides. It is never
   * published itself, so the published-dependent mapping of [roomPermission] does not apply.
   */
  private fun templatePermission(
      user: User,
      questionnaire: Questionnaire,
      permission: Any
  ): DomainPermissionEvaluation {
    val templatePermission =
        when (permission) {
          "read" -> "read"
          // A template has no moderators, so whoever may edit it moderates it.
          "moderate" -> "write"
          "write" -> "write"
          else -> return DomainPermissionEvaluation(false)
        }
    val template = templatePermissionEvaluator.findOneByKey(questionnaire.id!!)
    return templatePermissionEvaluator.hasPermission(user, template, templatePermission)
  }

  override fun findOneByKey(key: Any): Questionnaire {
    check(key is UUID) { "Excepted key to be of type UUID." }
    return questionnaireRepository.findByIdOrNull(key) ?: throw QuestionnaireNotFoundException(key)
  }

  override fun supports(clazz: KClass<out Any>) = clazz == Questionnaire::class

  override fun supports(className: String) = className == "Questionnaire"
}
