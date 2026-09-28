/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.round

import java.util.UUID
import kotlin.reflect.KClass
import net.particify.arsnova.core4.questionnaire.exception.RoundNotFoundException
import net.particify.arsnova.core4.security.DomainPermissionEvaluation
import net.particify.arsnova.core4.security.DomainPermissionEvaluator
import net.particify.arsnova.core4.user.User
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
class RoundPermissionEvaluator(
    private val roundRepository: RoundRepository,
) : DomainPermissionEvaluator<Round> {
  private val logger = LoggerFactory.getLogger(this::class.java)

  override fun hasPermission(
      user: User,
      targetDomainObject: Round,
      permission: Any
  ): DomainPermissionEvaluation {
    logger.debug("hasPermission({}, {}, {})", user, targetDomainObject, permission)
    val parentPermission =
        when (permission) {
          "write" -> "write"
          "read" -> "read"
          // Participants only see a round's statistics once the questionnaire publishes them.
          "readStatistics" ->
              if (targetDomainObject.content?.questionnaire?.statisticsPublished == true) "read"
              else "write"
          "delete" -> "write"
          else -> return DomainPermissionEvaluation(false)
        }
    return DomainPermissionEvaluation(
        null,
        DomainPermissionEvaluation.PermissionReference(
            "Content", targetDomainObject.content!!.id!!, parentPermission))
  }

  override fun findOneByKey(key: Any): Round {
    check(key is UUID) { "Excepted key to be of type UUID." }
    return roundRepository.findByIdOrNull(key) ?: throw RoundNotFoundException(key)
  }

  override fun supports(clazz: KClass<out Any>) = clazz == Round::class

  override fun supports(className: String) = className == "Round"
}
