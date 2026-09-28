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
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

@Controller
@PreAuthorize("hasRole('USER')")
@SchemaMapping(typeName = "Subscription")
class QuestionnaireSubscriptionController(
    private val questionnaireEventPublisher: QuestionnaireEventPublisher,
    private val questionnaireServiceImpl: QuestionnaireServiceImpl
) {

  @SubscriptionMapping
  @PreAuthorize("hasPermission(#questionnaireId, 'Questionnaire', 'read')")
  fun questionnaireUpdated(@Argument questionnaireId: UUID): Flux<Questionnaire> {
    return questionnaireEventPublisher
        .questionnaireUpdatedFlux()
        .filter { it == questionnaireId }
        .concatMap { fetchQuestionnaire(questionnaireId) }
  }

  @SubscriptionMapping
  @PreAuthorize("hasPermission(#roomId, 'Room', 'read')")
  fun questionnairesChanged(@Argument roomId: UUID): Flux<UUID> {
    return questionnaireEventPublisher
        .questionnairesChangedFlux()
        .filter { it.roomId == roomId }
        .map { it.questionnaireId }
  }

  private fun fetchQuestionnaire(questionnaireId: UUID): Mono<Questionnaire> {
    return Mono.fromCallable { questionnaireServiceImpl.findByIdOrNull(questionnaireId) }
        .subscribeOn(Schedulers.boundedElastic())
  }
}
