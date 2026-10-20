/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.content.api

import java.time.Duration
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import net.particify.arsnova.core4.questionnaire.content.Content
import net.particify.arsnova.core4.questionnaire.content.ContentPermissionEvaluator
import net.particify.arsnova.core4.questionnaire.content.ContentRepository
import net.particify.arsnova.core4.questionnaire.round.api.AnswerChangedEvent
import net.particify.arsnova.core4.questionnaire.round.api.RoundStatsEventPublisher
import net.particify.arsnova.core4.questionnaire.round.api.SharedFluxRegistry
import org.slf4j.LoggerFactory
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
class ContentSubscriptionController(
    private val roundStatsEventPublisher: RoundStatsEventPublisher,
    private val contentRepository: ContentRepository,
    private val contentPermissionEvaluator: ContentPermissionEvaluator,
    private val permissionEvaluator: PermissionEvaluator
) {

  companion object {
    private val STATS_UPDATE_SAMPLE_INTERVAL: Duration = Duration.ofSeconds(3)
  }

  private val logger = LoggerFactory.getLogger(javaClass)

  private data class ContentKey(val questionnaireId: UUID, val contentId: UUID)

  private val sharedContents =
      SharedFluxRegistry<ContentKey, Content>(
          STATS_UPDATE_SAMPLE_INTERVAL, replayToJoiners = false) { key ->
            roundStatsEventPublisher.answerChangedFlux().filter { it.contentId == key.contentId }
          }

  /**
   * Emits a content of the questionnaire whenever one of its answers changed, at most once per
   * sample interval per content. The first answer to a content joins the subscriber to the
   * content's computation, which all subscribers of the content share, see [SharedFluxRegistry]. A
   * subscriber joining it is not handed the content computed last: that predates the answer which
   * joined it, and might show a release withdrawn since, so it waits for the recomputation.
   *
   * Any content of the questionnaire can be answered, but participants may only see the released
   * ones, and release can change while subscribed, so every emission is checked against the content
   * it carries, which takes no database read. Whether the subscriber moderates the questionnaire,
   * and so sees every content, is decided once here, since the emissions run without the security
   * context: a membership revoked while subscribed only takes effect on the next subscription.
   *
   * A content whose computation failed to start is skipped rather than ending the subscription for
   * every other content, and the next answer to it joins it again.
   */
  @SubscriptionMapping
  @PreAuthorize("hasPermission(#questionnaireId, 'Questionnaire', 'read')")
  fun questionnaireContentUpdated(@Argument questionnaireId: UUID): Flux<Content> {
    val moderator = isModerator(questionnaireId)
    return Flux.defer {
          val joined = ConcurrentHashMap.newKeySet<UUID>()
          roundStatsEventPublisher
              .answerChangedFlux()
              .filter { event: AnswerChangedEvent -> event.questionnaireId == questionnaireId }
              .map { it.contentId }
              .filter { joined.add(it) }
              .flatMap { contentId ->
                sharedContents
                    .flux(ContentKey(questionnaireId, contentId)) {
                      contentRepository.findByIdOrNull(contentId)
                    }
                    .onErrorResume {
                      logger.warn("Skipped content {} after a failed computation.", contentId, it)
                      joined.remove(contentId)
                      Flux.empty()
                    }
              }
        }
        .filter { moderator || contentPermissionEvaluator.isVisibleToParticipants(it) }
  }

  /** Owners and editors moderate as well. */
  private fun isModerator(questionnaireId: UUID): Boolean {
    val authentication = SecurityContextHolder.getContext().authentication ?: return false
    return permissionEvaluator.hasPermission(
        authentication, questionnaireId, "Questionnaire", "moderate")
  }
}
