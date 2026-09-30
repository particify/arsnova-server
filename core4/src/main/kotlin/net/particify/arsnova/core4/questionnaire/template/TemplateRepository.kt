/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.template

import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.querydsl.QuerydslPredicateExecutor

interface TemplateRepository : JpaRepository<Template, UUID>, QuerydslPredicateExecutor<Template> {
  fun findByAuditMetadataCreatedByAndPublishedFalse(createdBy: UUID): List<Template>
}
