/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.round.api

import java.time.Duration
import java.util.UUID
import kotlin.reflect.KClass
import net.particify.arsnova.core4.questionnaire.content.Content
import net.particify.arsnova.core4.questionnaire.content.ContentPermissionEvaluator
import net.particify.arsnova.core4.questionnaire.content.SolutionVisibility
import net.particify.arsnova.core4.questionnaire.exception.RoundNotFoundException
import net.particify.arsnova.core4.questionnaire.round.ChoiceContentRoundStats
import net.particify.arsnova.core4.questionnaire.round.NumericContentRoundStats
import net.particify.arsnova.core4.questionnaire.round.RoundRepository
import net.particify.arsnova.core4.questionnaire.round.RoundStatsServiceImpl
import net.particify.arsnova.core4.questionnaire.round.ScaleContentRoundStats
import net.particify.arsnova.core4.questionnaire.round.ShortAnswerContentRoundStats
import net.particify.arsnova.core4.questionnaire.round.WordcloudContentRoundStats
import org.springframework.data.repository.findByIdOrNull
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.graphql.data.method.annotation.SubscriptionMapping
import org.springframework.security.access.PermissionEvaluator
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Controller
import reactor.core.publisher.Flux

@Controller
@PreAuthorize("hasRole('USER')")
@SchemaMapping(typeName = "Subscription")
class RoundStatsSubscriptionController(
    roundStatsEventPublisher: RoundStatsEventPublisher,
    private val roundStatsServiceImpl: RoundStatsServiceImpl,
    private val roundRepository: RoundRepository,
    private val contentPermissionEvaluator: ContentPermissionEvaluator,
    private val permissionEvaluator: PermissionEvaluator
) {

  companion object {
    private val STATS_UPDATE_SAMPLE_INTERVAL: Duration = Duration.ofSeconds(3)
  }

  /** Each subscription computes the stats of its own format, so the format is part of the key. */
  private data class StatsKey(val roundId: UUID, val format: KClass<*>)

  /** The stats along with their content, whose state decides who may see them. */
  private class StatsEmission(val content: Content, val stats: Any)

  private val sharedStats =
      SharedFluxRegistry<StatsKey, StatsEmission>(STATS_UPDATE_SAMPLE_INTERVAL) { key ->
        roundStatsEventPublisher.answerChangedFlux().filter { it.roundId == key.roundId }
      }

  @SubscriptionMapping
  @PreAuthorize("hasPermission(#roundId, 'Round', 'readStatistics')")
  fun questionnaireChoiceRoundStatsUpdated(@Argument roundId: UUID): Flux<ChoiceContentRoundStats> =
      statsFlux(roundId) { roundStatsServiceImpl.getChoiceRoundStatsByRoundId(roundId) }

  @SubscriptionMapping
  @PreAuthorize("hasPermission(#roundId, 'Round', 'readStatistics')")
  fun questionnaireNumericRoundStatsUpdated(
      @Argument roundId: UUID
  ): Flux<NumericContentRoundStats> =
      statsFlux(roundId, NumericContentRoundStats::withoutSolution) {
        roundStatsServiceImpl.getNumericRoundStatsWithSolutionByRoundId(roundId)
      }

  @SubscriptionMapping
  @PreAuthorize("hasPermission(#roundId, 'Round', 'readStatistics')")
  fun questionnaireScaleRoundStatsUpdated(@Argument roundId: UUID): Flux<ScaleContentRoundStats> =
      statsFlux(roundId) { roundStatsServiceImpl.getScaleRoundStatsByRoundId(roundId) }

  @SubscriptionMapping
  @PreAuthorize("hasPermission(#roundId, 'Round', 'readStatistics')")
  fun questionnaireShortAnswerRoundStatsUpdated(
      @Argument roundId: UUID
  ): Flux<ShortAnswerContentRoundStats> =
      statsFlux(roundId, ShortAnswerContentRoundStats::withoutSolution) {
        roundStatsServiceImpl.getShortAnswerRoundStatsWithSolutionByRoundId(roundId)
      }

  @SubscriptionMapping
  @PreAuthorize("hasPermission(#roundId, 'Round', 'readStatistics')")
  fun questionnaireWordcloudRoundStatsUpdated(
      @Argument roundId: UUID
  ): Flux<WordcloudContentRoundStats> =
      statsFlux(roundId) { roundStatsServiceImpl.getWordcloudRoundStatsByRoundId(roundId) }

  /**
   * Emits the round's stats, starting with the current ones and then whenever one of its answers
   * changed, at most once per sample interval. All subscribers of a round share one computation,
   * see [SharedFluxRegistry].
   *
   * Publishing of the statistics and of the solution, as well as release of the content, can change
   * while subscribed, so every emission is checked against the content it carries, which takes no
   * database read. The shared stats include the solution, which [withoutSolution] leaves out for a
   * subscriber who may not see it yet. Whether the subscriber moderates the questionnaire, and so
   * sees both regardless, is decided once here, since the emissions run without the security
   * context: a membership revoked while subscribed only takes effect on the next subscription.
   */
  private inline fun <reified T : Any> statsFlux(
      roundId: UUID,
      noinline withoutSolution: (T) -> T = { it },
      noinline supplier: () -> T
  ): Flux<T> {
    val moderator = isModerator(roundId)
    return sharedStats
        .flux(StatsKey(roundId, T::class)) { StatsEmission(findContent(roundId), supplier()) }
        .filter { moderator || maySeeStatistics(it.content) }
        .map { emission ->
          val stats = emission.stats as T
          if (moderator || isSolutionPublished(emission.content)) stats else withoutSolution(stats)
        }
  }

  private fun maySeeStatistics(content: Content): Boolean =
      contentPermissionEvaluator.areStatisticsPublishedToParticipants(content)

  /** For everyone but moderators, as in [SolutionVisibility.maySeeSolution]. */
  private fun isSolutionPublished(content: Content): Boolean =
      content.questionnaire?.correctOptionsPublished == true

  /** Owners and editors moderate as well. */
  private fun isModerator(roundId: UUID): Boolean {
    val authentication = SecurityContextHolder.getContext().authentication ?: return false
    val questionnaire = findContent(roundId).questionnaire ?: return false
    return permissionEvaluator.hasPermission(authentication, questionnaire, "moderate")
  }

  private fun findContent(roundId: UUID): Content =
      roundRepository.findByIdOrNull(roundId)?.content ?: throw RoundNotFoundException(roundId)
}
