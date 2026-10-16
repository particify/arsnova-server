/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.answer

import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface AnswerRepository : JpaRepository<Answer, UUID> {
  fun findByRoundIdAndAuditMetadataCreatedBy(roundId: UUID, createdBy: UUID): Answer?

  fun findByRoundId(roundId: UUID): List<Answer>

  fun findByRoundIdInAndAuditMetadataCreatedBy(
      roundIds: Collection<UUID>,
      createdBy: UUID
  ): List<Answer>

  fun findByRoundIdAndHiddenFalseOrderByIdAsc(roundId: UUID): List<Answer>

  fun countByRoundIdAndHiddenFalse(roundId: UUID): Int

  fun countByRoundIdAndHiddenFalseAndResult(roundId: UUID, result: AnswerResultState): Int

  /** Hidden answers count too: they are still answers an edit would reinterpret. */
  @Query("SELECT DISTINCT a.round.content.id FROM Answer a WHERE a.round.content.id IN :contentIds")
  fun findContentIdsWithAnswers(contentIds: Collection<UUID>): Set<UUID>

  fun deleteByRoundId(roundId: UUID): Int
}
