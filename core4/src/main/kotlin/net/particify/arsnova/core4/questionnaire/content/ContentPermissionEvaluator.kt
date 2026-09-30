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
          "read" -> if (isVisible(targetDomainObject, questionnaire)) "read" else "moderate"
          // Participants only see the statistics once the questionnaire publishes them, while
          // moderators see them throughout, as they do the answer counts.
          "readStatistics" ->
              if (areStatisticsPublished(targetDomainObject, questionnaire)) "read" else "moderate"
          "delete" -> "write"
          else -> return DomainPermissionEvaluation(false)
        }
    return DomainPermissionEvaluation(
        null,
        DomainPermissionEvaluation.PermissionReference(
            "Questionnaire", questionnaire.id!!, parentPermission))
  }

  /** A template's contents are all visible to whoever may read the template. */
  private fun isVisible(content: Content, questionnaire: Questionnaire): Boolean =
      questionnaire.isRoomless() || (questionnaire.published && isReleased(content, questionnaire))

  private fun areStatisticsPublished(content: Content, questionnaire: Questionnaire): Boolean =
      questionnaire.statisticsPublished == true && isVisible(content, questionnaire)

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

  /**
   * Whether participants may read [content], decided from the entity alone, for subscribers who
   * re-check a content they have been handed rather than reading it again.
   */
  fun isVisibleToParticipants(content: Content): Boolean =
      isVisible(content, content.questionnaire!!)

  /** Whether participants may read the statistics of [content], decided from the entity alone. */
  fun areStatisticsPublishedToParticipants(content: Content): Boolean =
      areStatisticsPublished(content, content.questionnaire!!)

  override fun findOneByKey(key: Any): Content {
    check(key is UUID) { "Excepted key to be of type UUID." }
    return contentRepository.findByIdOrNull(key) ?: throw ContentNotFoundException(key)
  }

  override fun supports(clazz: KClass<out Any>) = clazz == Content::class

  override fun supports(className: String) = className == "Content"
}
