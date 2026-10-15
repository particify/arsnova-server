/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.answer

import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository

interface AnswerRepository : JpaRepository<Answer, UUID> {
  fun findByRoundIdAndAuditMetadataCreatedBy(roundId: UUID, createdBy: UUID): Answer?

  fun findByRoundId(roundId: UUID): List<Answer>

  fun findByRoundIdAndHiddenFalseOrderByIdAsc(roundId: UUID): List<Answer>

  fun countByRoundIdAndHiddenFalse(roundId: UUID): Int

  fun countByRoundIdAndHiddenFalseAndResult(roundId: UUID, result: AnswerResultState): Int

  fun existsByRoundId(roundId: UUID): Boolean

  fun deleteByRoundId(roundId: UUID): Int
}
