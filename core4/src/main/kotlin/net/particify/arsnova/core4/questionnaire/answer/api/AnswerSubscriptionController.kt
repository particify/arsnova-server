/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.answer.api

import java.time.Duration
import java.util.UUID
import net.particify.arsnova.core4.questionnaire.answer.Answer
import net.particify.arsnova.core4.questionnaire.answer.AnswerServiceImpl
import net.particify.arsnova.core4.questionnaire.round.api.AnswerChangedEvent
import net.particify.arsnova.core4.questionnaire.round.api.RoundStatsEventPublisher
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.graphql.data.method.annotation.SubscriptionMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.concurrent.DelegatingSecurityContextCallable
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Controller
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

@Controller
@PreAuthorize("hasRole('USER')")
@SchemaMapping(typeName = "Subscription")
class AnswerSubscriptionController(
    private val roundStatsEventPublisher: RoundStatsEventPublisher,
    private val answerServiceImpl: AnswerServiceImpl
) {

  companion object {
    private val ANSWERS_UPDATE_SAMPLE_INTERVAL: Duration = Duration.ofSeconds(3)
  }

  /**
   * Emits the answers visible to the subscriber whenever the round's answer set changes, so that
   * both newly submitted and newly hidden answers reach every viewer.
   *
   * The subscriber's authentication is snapshotted here, while still on the thread that carries the
   * security context, and reinstated for every fetch. Emissions run on another thread where the
   * thread-bound context is absent, and the visibility gating in
   * [AnswerServiceImpl.findTextAnswersByRoundId] reads it to decide whether the caller may see
   * unpublished and hidden answers. Permissions are therefore evaluated as of subscription time; a
   * role change only takes effect once the client resubscribes.
   */
  @SubscriptionMapping
  @PreAuthorize("hasPermission(#roundId, 'Round', 'read')")
  fun questionnaireTextAnswersUpdated(@Argument roundId: UUID): Flux<List<Answer>> {
    val securityContext = snapshotSecurityContext()
    return roundStatsEventPublisher
        .answerChangedFlux()
        .filter { event: AnswerChangedEvent -> event.roundId == roundId }
        .sample(ANSWERS_UPDATE_SAMPLE_INTERVAL)
        .concatMap { fetchTextAnswers(roundId, securityContext) }
  }

  private fun snapshotSecurityContext(): SecurityContext {
    val snapshot = SecurityContextHolder.createEmptyContext()
    snapshot.authentication = SecurityContextHolder.getContext().authentication
    return snapshot
  }

  private fun fetchTextAnswers(
      roundId: UUID,
      securityContext: SecurityContext
  ): Mono<List<Answer>> {
    val callable = { answerServiceImpl.findTextAnswersByRoundId(roundId) }
    return Mono.fromCallable(DelegatingSecurityContextCallable(callable, securityContext))
        .subscribeOn(Schedulers.boundedElastic())
  }
}
