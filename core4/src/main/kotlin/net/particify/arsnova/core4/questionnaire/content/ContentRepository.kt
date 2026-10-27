/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.content

import java.util.UUID
import org.springframework.data.domain.ScrollPosition
import org.springframework.data.domain.Window
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface ContentRepository : JpaRepository<Content, UUID> {
  fun findByQuestionnaireIdOrderByPosition(
      questionnaireId: UUID,
      scrollPosition: ScrollPosition
  ): Window<Content>

  fun findAllByQuestionnaireIdOrderByPosition(questionnaireId: UUID): List<Content>

  fun countByQuestionnaireId(questionnaireId: UUID): Int

  @Query(
      "SELECT c.questionnaire.id, COUNT(c) FROM Content c " +
          "WHERE c.questionnaire.id IN :questionnaireIds GROUP BY c.questionnaire.id")
  fun countByQuestionnaireIds(questionnaireIds: Collection<UUID>): List<Array<Any>>

  /** Matches [pattern] with ILIKE, so its wildcards must already be escaped. */
  @Query(
      value =
          "SELECT DISTINCT q.id FROM questionnaire.questionnaire q " +
              "JOIN questionnaire.content c ON c.questionnaire_id = q.id " +
              "WHERE q.room_id IS NULL AND (c.body ILIKE :pattern " +
              "OR c.format_data ->> 'options' ILIKE :pattern " +
              "OR c.format_data ->> 'answer' ILIKE :pattern)",
      nativeQuery = true)
  fun findRoomlessQuestionnaireIdsByContentTextLike(pattern: String): List<UUID>

  fun deleteByQuestionnaireId(questionnaireId: UUID): Int
}
