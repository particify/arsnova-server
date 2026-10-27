/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.template

import java.util.UUID
import kotlin.reflect.KClass
import net.particify.arsnova.core4.questionnaire.exception.TemplateNotFoundException
import net.particify.arsnova.core4.security.DomainPermissionEvaluation
import net.particify.arsnova.core4.security.DomainPermissionEvaluator
import net.particify.arsnova.core4.user.User
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

/**
 * Published templates are readable by everyone, and only their creator may change them. The
 * questionnaire holding a template's contents refers its permissions here.
 */
@Component
class TemplatePermissionEvaluator(private val templateRepository: TemplateRepository) :
    DomainPermissionEvaluator<Template> {
  private val logger = LoggerFactory.getLogger(this::class.java)

  override fun hasPermission(
      user: User,
      targetDomainObject: Template,
      permission: Any
  ): DomainPermissionEvaluation {
    logger.debug("hasPermission({}, {}, {})", user, targetDomainObject, permission)
    val isCreator = user.id != null && targetDomainObject.auditMetadata.createdBy == user.id
    val granted =
        when (permission) {
          "read" -> targetDomainObject.published || isCreator
          "write",
          "delete" -> isCreator
          else -> false
        }
    return DomainPermissionEvaluation(granted)
  }

  override fun findOneByKey(key: Any): Template {
    check(key is UUID) { "Excepted key to be of type UUID." }
    return templateRepository.findByIdOrNull(key) ?: throw TemplateNotFoundException(key)
  }

  override fun supports(clazz: KClass<out Any>) = clazz == Template::class

  override fun supports(className: String) = className == "Template"
}
