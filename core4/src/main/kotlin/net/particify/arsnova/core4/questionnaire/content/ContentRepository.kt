/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.content

import java.util.UUID
import org.springframework.data.domain.ScrollPosition
import org.springframework.data.domain.Window
import org.springframework.data.jpa.repository.JpaRepository

interface ContentRepository : JpaRepository<Content, UUID> {
  fun findByQuestionnaireIdOrderByPosition(
      questionnaireId: UUID,
      scrollPosition: ScrollPosition
  ): Window<Content>

  fun countByQuestionnaireId(questionnaireId: UUID): Int

  fun deleteByQuestionnaireId(questionnaireId: UUID): Int
}
