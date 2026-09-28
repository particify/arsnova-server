/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.round.api

import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import net.particify.arsnova.core4.questionnaire.round.ChoiceContentRoundStats
import net.particify.arsnova.core4.questionnaire.round.NumericContentRoundStats
import net.particify.arsnova.core4.questionnaire.round.Round
import net.particify.arsnova.core4.questionnaire.round.RoundServiceImpl
import net.particify.arsnova.core4.questionnaire.round.RoundStatsServiceImpl
import net.particify.arsnova.core4.questionnaire.round.ScaleContentRoundStats
import net.particify.arsnova.core4.questionnaire.round.WordcloudContentRoundStats
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Controller

@Controller
@PreAuthorize("hasRole('USER')")
@SchemaMapping(typeName = "Query")
class RoundQueryController(
    private val roundServiceImpl: RoundServiceImpl,
    private val roundStatsServiceImpl: RoundStatsServiceImpl
) {
  @QueryMapping
  @PreAuthorize("hasPermission(#contentId, 'Content', 'read')")
  fun questionnaireRoundsByContentId(@Argument contentId: UUID): List<Round> {
    return roundServiceImpl.findByContentIdOrderByRoundNo(contentId)
  }

  @QueryMapping
  @PreAuthorize("hasPermission(#roundId, 'Round', 'readStatistics')")
  fun questionnaireChoiceRoundStatsByRoundId(@Argument roundId: UUID): ChoiceContentRoundStats {
    return roundStatsServiceImpl.getChoiceRoundStatsByRoundId(roundId)
  }

  @QueryMapping
  @PreAuthorize("hasPermission(#roundId, 'Round', 'readStatistics')")
  fun questionnaireNumericRoundStatsByRoundId(@Argument roundId: UUID): NumericContentRoundStats {
    return roundStatsServiceImpl.getNumericRoundStatsByRoundId(
        roundId, SecurityContextHolder.getContext().authentication)
  }

  @QueryMapping
  @PreAuthorize("hasPermission(#roundId, 'Round', 'readStatistics')")
  fun questionnaireScaleRoundStatsByRoundId(@Argument roundId: UUID): ScaleContentRoundStats {
    return roundStatsServiceImpl.getScaleRoundStatsByRoundId(roundId)
  }

  @QueryMapping
  @PreAuthorize("hasPermission(#roundId, 'Round', 'readStatistics')")
  fun questionnaireWordcloudRoundStatsByRoundId(
      @Argument roundId: UUID
  ): WordcloudContentRoundStats {
    return roundStatsServiceImpl.getWordcloudRoundStatsByRoundId(roundId)
  }

  @SchemaMapping(typeName = "Round")
  fun answeringEndTime(round: Round): OffsetDateTime? {
    return round.answeringEndTime?.atOffset(ZoneOffset.UTC)
  }

  @SchemaMapping(typeName = "Round")
  fun contentId(round: Round): UUID {
    return round.content!!.id!!
  }

  @SchemaMapping(typeName = "Round")
  fun answerable(round: Round): Boolean {
    return round.acceptsAnswers()
  }
}
