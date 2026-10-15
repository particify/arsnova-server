/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.round.api

import java.time.Duration
import java.util.UUID
import net.particify.arsnova.core4.questionnaire.round.ChoiceContentRoundStats
import net.particify.arsnova.core4.questionnaire.round.RoundStatsServiceImpl
import net.particify.arsnova.core4.questionnaire.round.WordcloudContentRoundStats
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.graphql.data.method.annotation.SubscriptionMapping
import org.springframework.security.access.PermissionEvaluator
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Controller
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

@Controller
@PreAuthorize("hasRole('USER')")
@SchemaMapping(typeName = "Subscription")
class RoundStatsSubscriptionController(
    private val roundStatsEventPublisher: RoundStatsEventPublisher,
    private val roundStatsServiceImpl: RoundStatsServiceImpl,
    private val permissionEvaluator: PermissionEvaluator
) {

  companion object {
    private val STATS_UPDATE_SAMPLE_INTERVAL: Duration = Duration.ofSeconds(3)
  }

  @SubscriptionMapping
  @PreAuthorize("hasPermission(#roundId, 'Round', 'readStatistics')")
  fun questionnaireChoiceRoundStatsUpdated(@Argument roundId: UUID): Flux<ChoiceContentRoundStats> =
      statsFlux(roundId) { roundStatsServiceImpl.getChoiceRoundStatsByRoundId(roundId) }

  @SubscriptionMapping
  @PreAuthorize("hasPermission(#roundId, 'Round', 'readStatistics')")
  fun questionnaireWordcloudRoundStatsUpdated(
      @Argument roundId: UUID
  ): Flux<WordcloudContentRoundStats> =
      statsFlux(roundId) { roundStatsServiceImpl.getWordcloudRoundStatsByRoundId(roundId) }

  /**
   * Re-reads the round's stats whenever one of its answers changed, at most once per sample
   * interval. [supplier] blocks on the database, so it is kept off the event thread. Publishing of
   * the statistics, release of the content and permissions can change while subscribed, so the
   * permission is checked on every emission, with the authentication captured at subscription since
   * the emission thread has none.
   */
  private fun <T : Any> statsFlux(roundId: UUID, supplier: () -> T): Flux<T> {
    val authentication = SecurityContextHolder.getContext().authentication ?: return Flux.empty()
    return roundStatsEventPublisher
        .answerChangedFlux()
        .filter { event: AnswerChangedEvent -> event.roundId == roundId }
        .sample(STATS_UPDATE_SAMPLE_INTERVAL)
        .concatMap {
          Mono.fromCallable {
                permissionEvaluator.hasPermission(
                    authentication, roundId, "Round", "readStatistics")
              }
              .filter { it }
              .map { supplier() }
              .subscribeOn(Schedulers.boundedElastic())
        }
  }
}
