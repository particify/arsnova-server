/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.content.api

import java.time.Duration
import java.util.UUID
import net.particify.arsnova.core4.questionnaire.content.Content
import net.particify.arsnova.core4.questionnaire.content.ContentRepository
import net.particify.arsnova.core4.questionnaire.round.api.AnswerChangedEvent
import net.particify.arsnova.core4.questionnaire.round.api.RoundStatsEventPublisher
import org.springframework.data.repository.findByIdOrNull
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.graphql.data.method.annotation.SubscriptionMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

@Controller
@PreAuthorize("hasRole('USER')")
@SchemaMapping(typeName = "Subscription")
class ContentSubscriptionController(
    private val roundStatsEventPublisher: RoundStatsEventPublisher,
    private val contentRepository: ContentRepository
) {

  companion object {
    private val STATS_UPDATE_SAMPLE_INTERVAL: Duration = Duration.ofSeconds(3)
  }

  @SubscriptionMapping
  @PreAuthorize("hasPermission(#questionnaireId, 'Questionnaire', 'read')")
  fun questionnaireContentUpdated(@Argument questionnaireId: UUID): Flux<Content> {
    return roundStatsEventPublisher
        .answerChangedFlux()
        .filter { event: AnswerChangedEvent -> event.questionnaireId == questionnaireId }
        .groupBy { it.contentId }
        .flatMap { group -> group.sample(STATS_UPDATE_SAMPLE_INTERVAL) }
        .concatMap { fetchContent(it.contentId) }
  }

  private fun fetchContent(contentId: UUID): Mono<Content> {
    return Mono.fromCallable { contentRepository.findByIdOrNull(contentId) }
        .mapNotNull { it }
        .subscribeOn(Schedulers.boundedElastic())
  }
}
