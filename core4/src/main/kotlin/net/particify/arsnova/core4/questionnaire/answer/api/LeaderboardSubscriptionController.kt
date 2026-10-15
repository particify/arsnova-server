/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.answer.api

import java.util.Locale
import java.util.UUID
import net.particify.arsnova.core4.questionnaire.answer.LeaderboardPage
import net.particify.arsnova.core4.questionnaire.answer.LeaderboardServiceImpl
import net.particify.arsnova.core4.user.User
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.graphql.data.method.annotation.SubscriptionMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller
import reactor.core.publisher.Flux

@Controller
@PreAuthorize("hasRole('USER')")
@SchemaMapping(typeName = "Subscription")
class LeaderboardSubscriptionController(
    private val leaderboardFluxRegistry: LeaderboardFluxRegistry,
    private val leaderboardServiceImpl: LeaderboardServiceImpl
) {

  /**
   * Emits the leaderboard whenever one of the questionnaire's answers changed, at most once per
   * sample interval. All subscribers of a questionnaire share one computation; only cutting it to
   * size and localizing the aliases happens per subscriber, and neither touches the database.
   *
   * A content of another questionnaire is rejected before subscribing, rather than failing the
   * shared computation once the first answer arrives.
   */
  @SubscriptionMapping
  @PreAuthorize("hasPermission(#questionnaireId, 'Questionnaire', 'readLeaderboard')")
  fun questionnaireLeaderboardUpdated(
      @Argument questionnaireId: UUID,
      @Argument contentId: UUID?,
      @Argument limit: Int?,
      @AuthenticationPrincipal user: User,
      locale: Locale
  ): Flux<LeaderboardPage> {
    contentId?.let { leaderboardServiceImpl.requireContentOfQuestionnaire(questionnaireId, it) }
    val userId = user.id
    // Decided here since the emissions run without a security context.
    val maySeeUnpublishedResults =
        leaderboardServiceImpl.maySeeUnpublishedResults(questionnaireId, contentId)
    return leaderboardFluxRegistry.leaderboardFlux(questionnaireId, contentId).map {
      leaderboardServiceImpl.toPage(it, userId, limit, locale, maySeeUnpublishedResults)
    }
  }
}
