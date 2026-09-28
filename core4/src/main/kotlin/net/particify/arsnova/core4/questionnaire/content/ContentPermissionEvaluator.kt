/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.content

import java.util.UUID
import kotlin.reflect.KClass
import net.particify.arsnova.core4.questionnaire.exception.ContentNotFoundException
import net.particify.arsnova.core4.questionnaire.questionnaire.Questionnaire
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnairePublishingMode
import net.particify.arsnova.core4.security.DomainPermissionEvaluation
import net.particify.arsnova.core4.security.DomainPermissionEvaluator
import net.particify.arsnova.core4.user.User
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
class ContentPermissionEvaluator(
    private val contentRepository: ContentRepository,
) : DomainPermissionEvaluator<Content> {
  private val logger = LoggerFactory.getLogger(this::class.java)

  override fun hasPermission(
      user: User,
      targetDomainObject: Content,
      permission: Any
  ): DomainPermissionEvaluation {
    logger.debug("hasPermission({}, {}, {})", user, targetDomainObject, permission)
    val questionnaire = targetDomainObject.questionnaire!!
    val parentPermission =
        when (permission) {
          "write" -> "write"
          "read" ->
              if (questionnaire.published && isReleased(targetDomainObject, questionnaire)) "read"
              else "write"
          "delete" -> "write"
          else -> return DomainPermissionEvaluation(false)
        }
    return DomainPermissionEvaluation(
        null,
        DomainPermissionEvaluation.PermissionReference(
            "Questionnaire", questionnaire.id!!, parentPermission))
  }

  private fun isReleased(content: Content, questionnaire: Questionnaire): Boolean {
    return when (questionnaire.publishingMode) {
      QuestionnairePublishingMode.ALL -> true
      QuestionnairePublishingMode.UP_TO,
      QuestionnairePublishingMode.LIVE -> {
        val releasedPosition = questionnaire.publishedUpToContent?.position ?: return false
        content.position!! <= releasedPosition
      }
      null -> false
    }
  }

  override fun findOneByKey(key: Any): Content {
    check(key is UUID) { "Excepted key to be of type UUID." }
    return contentRepository.findByIdOrNull(key) ?: throw ContentNotFoundException(key)
  }

  override fun supports(clazz: KClass<out Any>) = clazz == Content::class

  override fun supports(className: String) = className == "Content"
}
