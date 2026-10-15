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
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller

@Controller
@PreAuthorize("hasRole('USER')")
@SchemaMapping(typeName = "Query")
class LeaderboardQueryController(private val leaderboardServiceImpl: LeaderboardServiceImpl) {

  @QueryMapping
  @PreAuthorize("hasPermission(#questionnaireId, 'Questionnaire', 'readLeaderboard')")
  fun questionnaireLeaderboardByQuestionnaireId(
      @Argument questionnaireId: UUID,
      @Argument contentId: UUID?,
      @Argument limit: Int?,
      @AuthenticationPrincipal user: User,
      locale: Locale
  ): LeaderboardPage {
    val entries =
        leaderboardServiceImpl.findLeaderboardByQuestionnaireId(questionnaireId, contentId)
    val maySeeUnpublishedResults =
        leaderboardServiceImpl.maySeeUnpublishedResults(questionnaireId, contentId)
    return leaderboardServiceImpl.toPage(entries, user.id, limit, locale, maySeeUnpublishedResults)
  }
}
