/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.questionnaire

import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface QuestionnaireRepository : JpaRepository<Questionnaire, UUID> {
  // Names are only unique case-sensitively, so the id keeps the order of case variants stable.
  @Query("SELECT q FROM Questionnaire q WHERE q.roomId = :roomId ORDER BY LOWER(q.name), q.id")
  fun findByRoomIdOrderByName(roomId: UUID): List<Questionnaire>

  @Query(
      "SELECT q FROM Questionnaire q WHERE q.roomId = :roomId AND q.published = true " +
          "ORDER BY LOWER(q.name), q.id")
  fun findPublishedByRoomIdOrderByName(roomId: UUID): List<Questionnaire>

  fun findByRoomIdAndName(roomId: UUID, name: String): Questionnaire?
}
