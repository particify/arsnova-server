/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.answer

import jakarta.persistence.Tuple
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

  fun findByRoundIdOrderByIdAsc(roundId: UUID): List<Answer>

  fun findByRoundIdAndHiddenFalseOrderByIdAsc(roundId: UUID): List<Answer>

  /** Sums the scores of the questionnaire's active rounds per user in a single query. */
  @Query(
      "SELECT a.auditMetadata.createdBy, SUM(a.points) FROM Answer a " +
          "WHERE a.round.id IN " +
          "(SELECT c.activeRound.id FROM Content c WHERE c.questionnaire.id = :questionnaireId) " +
          "AND a.auditMetadata.createdBy IS NOT NULL " +
          "GROUP BY a.auditMetadata.createdBy")
  fun sumPointsByQuestionnaireIdGroupByCreatedBy(questionnaireId: UUID): List<Tuple>

  fun countByRoundIdAndHiddenFalse(roundId: UUID): Int

  fun countByRoundIdAndHiddenFalseAndResult(roundId: UUID, result: AnswerResultState): Int

  /** Hidden answers count too: they are still answers an edit would reinterpret. */
  @Query("SELECT DISTINCT a.round.content.id FROM Answer a WHERE a.round.content.id IN :contentIds")
  fun findContentIdsWithAnswers(contentIds: Collection<UUID>): Set<UUID>

  fun deleteByRoundId(roundId: UUID): Int
}
