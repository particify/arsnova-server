/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.questionnaire.api

import java.util.UUID
import net.particify.arsnova.core4.questionnaire.questionnaire.Questionnaire
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnaireServiceImpl
import org.springframework.data.repository.findByIdOrNull
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.graphql.data.method.annotation.SubscriptionMapping
import org.springframework.security.access.PermissionEvaluator
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Controller
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

@Controller
@PreAuthorize("hasRole('USER')")
@SchemaMapping(typeName = "Subscription")
class QuestionnaireSubscriptionController(
    private val questionnaireEventPublisher: QuestionnaireEventPublisher,
    private val questionnaireServiceImpl: QuestionnaireServiceImpl,
    private val permissionEvaluator: PermissionEvaluator
) {

  @SubscriptionMapping
  @PreAuthorize("hasPermission(#questionnaireId, 'Questionnaire', 'read')")
  fun questionnaireUpdated(@Argument questionnaireId: UUID): Flux<Questionnaire> {
    val authentication = SecurityContextHolder.getContext().authentication ?: return Flux.empty()
    return questionnaireEventPublisher
        .questionnaireUpdatedFlux()
        .filter { it == questionnaireId }
        .concatMap { fetchReadableQuestionnaire(questionnaireId, authentication) }
  }

  @SubscriptionMapping
  @PreAuthorize("hasPermission(#roomId, 'Room', 'read')")
  fun questionnairesChanged(@Argument roomId: UUID): Flux<UUID> {
    return questionnaireEventPublisher
        .questionnairesChangedFlux()
        .filter { it.roomId == roomId }
        .map { it.questionnaireId }
  }

  /**
   * The questionnaire can be unpublished while subscribed, so read permission is checked on every
   * emission rather than only at subscription. Its released range needs no check of its own: the
   * range's end is released by definition once the questionnaire is readable.
   */
  private fun fetchReadableQuestionnaire(
      questionnaireId: UUID,
      authentication: Authentication
  ): Mono<Questionnaire> {
    return Mono.fromCallable {
          questionnaireServiceImpl.findByIdOrNull(questionnaireId)?.takeIf {
            permissionEvaluator.hasPermission(authentication, it, "read")
          }
        }
        .subscribeOn(Schedulers.boundedElastic())
  }
}
