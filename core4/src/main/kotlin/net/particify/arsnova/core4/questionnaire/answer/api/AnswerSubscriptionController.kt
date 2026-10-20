/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.answer.api

import java.time.Duration
import java.util.UUID
import net.particify.arsnova.core4.questionnaire.answer.Answer
import net.particify.arsnova.core4.questionnaire.answer.AnswerServiceImpl
import net.particify.arsnova.core4.questionnaire.content.Content
import net.particify.arsnova.core4.questionnaire.content.ContentPermissionEvaluator
import net.particify.arsnova.core4.questionnaire.content.SolutionVisibility
import net.particify.arsnova.core4.questionnaire.content.TextContentFormatData
import net.particify.arsnova.core4.questionnaire.exception.ContentFormatDataInvalidException
import net.particify.arsnova.core4.questionnaire.exception.RoundNotFoundException
import net.particify.arsnova.core4.questionnaire.round.RoundRepository
import net.particify.arsnova.core4.questionnaire.round.api.RoundStatsEventPublisher
import net.particify.arsnova.core4.questionnaire.round.api.SharedFluxRegistry
import org.springframework.data.repository.findByIdOrNull
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.graphql.data.method.annotation.SubscriptionMapping
import org.springframework.security.access.PermissionEvaluator
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller
import reactor.core.publisher.Flux

@Controller
@PreAuthorize("hasRole('USER')")
@SchemaMapping(typeName = "Subscription")
class AnswerSubscriptionController(
    roundStatsEventPublisher: RoundStatsEventPublisher,
    private val answerServiceImpl: AnswerServiceImpl,
    private val roundRepository: RoundRepository,
    private val contentPermissionEvaluator: ContentPermissionEvaluator,
    private val permissionEvaluator: PermissionEvaluator
) {

  companion object {
    private val ANSWERS_UPDATE_SAMPLE_INTERVAL: Duration = Duration.ofSeconds(3)
  }

  /** All answers of a round, hidden ones included, along with their content. */
  private class TextAnswersEmission(val content: Content, val answers: List<Answer>)

  private val sharedAnswers =
      SharedFluxRegistry<UUID, TextAnswersEmission>(
          ANSWERS_UPDATE_SAMPLE_INTERVAL,
          ends = { key ->
            roundStatsEventPublisher
                .roundDeletedFlux()
                .filter { it == key }
                .map { RoundNotFoundException(it) }
          }) { roundId ->
            roundStatsEventPublisher.answerChangedFlux().filter { it.roundId == roundId }
          }

  /**
   * Emits the answers visible to the subscriber, starting with the current ones and then whenever
   * the round's answer set changes, at most once per sample interval, so that both newly submitted
   * and newly hidden answers reach every viewer. All subscribers of a round share one computation
   * of all of its answers, see [SharedFluxRegistry], and each gets those
   * [AnswerServiceImpl.findTextAnswersByRoundId] would return to them. Deleting the round ends the
   * stream with [RoundNotFoundException].
   *
   * Whether the subscriber may edit the content or moderate its room, and so sees hidden and
   * unpublished answers of an unreleased content too, is decided once here, since the emissions run
   * without the security context: a membership revoked while subscribed only takes effect on the
   * next subscription. Release of the content and publishing of its answers can change while
   * subscribed, so every emission is checked against the content it carries, which takes no
   * database read.
   */
  @SubscriptionMapping
  @PreAuthorize("hasPermission(#roundId, 'Round', 'read')")
  fun questionnaireTextAnswersUpdated(@Argument roundId: UUID): Flux<List<Answer>> {
    val privileged = SolutionVisibility.isPrivileged(permissionEvaluator, findContent(roundId))
    return sharedAnswers
        .flux(roundId) { findAllTextAnswers(roundId) }
        .filter { privileged || contentPermissionEvaluator.isVisibleToParticipants(it.content) }
        .map { visibleAnswers(it, privileged) }
  }

  private fun findAllTextAnswers(roundId: UUID): TextAnswersEmission {
    val content = findContent(roundId)
    if (content.formatData !is TextContentFormatData) {
      throw ContentFormatDataInvalidException("Content is not a text content")
    }
    return TextAnswersEmission(content, answerServiceImpl.findByRoundIdOrderByIdAsc(roundId))
  }

  /** For participants an unpublished answer set is empty rather than an error. */
  private fun visibleAnswers(emission: TextAnswersEmission, privileged: Boolean): List<Answer> =
      when {
        privileged -> emission.answers
        (emission.content.formatData as? TextContentFormatData)?.answersPublished == true ->
            emission.answers.filterNot { it.hidden }
        else -> emptyList()
      }

  private fun findContent(roundId: UUID): Content =
      roundRepository.findByIdOrNull(roundId)?.content ?: throw RoundNotFoundException(roundId)
}
