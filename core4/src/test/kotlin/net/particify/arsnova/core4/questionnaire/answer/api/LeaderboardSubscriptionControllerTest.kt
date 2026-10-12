/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.answer.api

import java.util.Locale
import java.util.UUID
import net.particify.arsnova.core4.questionnaire.answer.LeaderboardPage
import net.particify.arsnova.core4.questionnaire.answer.LeaderboardServiceImpl
import net.particify.arsnova.core4.questionnaire.answer.RawLeaderboardEntry
import net.particify.arsnova.core4.questionnaire.exception.ContentNotFoundException
import net.particify.arsnova.core4.user.User
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import reactor.core.publisher.Flux
import reactor.test.StepVerifier

private val QUESTIONNAIRE_ID = UUID.randomUUID()
private val CONTENT_ID = UUID.randomUUID()
private val LOCALE = Locale.of("de")
private const val LIMIT = 5

class LeaderboardSubscriptionControllerTest {
  private val registry = Mockito.mock(LeaderboardFluxRegistry::class.java)
  private val leaderboardService = Mockito.mock(LeaderboardServiceImpl::class.java)
  private val controller = LeaderboardSubscriptionController(registry, leaderboardService)

  @Test
  fun shouldCutEachSharedRankingToThePageOfTheSubscriber() {
    val user = User(id = UUID.randomUUID())
    val first = listOf(RawLeaderboardEntry(user.id!!, null, 500, 1, null))
    val second = listOf<RawLeaderboardEntry>()
    val firstPage = LeaderboardPage(listOf(), null, 1)
    val secondPage = LeaderboardPage(listOf(), null, 0)
    Mockito.`when`(registry.leaderboardFlux(QUESTIONNAIRE_ID, CONTENT_ID))
        .thenReturn(Flux.just(first, second))
    Mockito.`when`(leaderboardService.toPage(first, user.id, LIMIT, LOCALE)).thenReturn(firstPage)
    Mockito.`when`(leaderboardService.toPage(second, user.id, LIMIT, LOCALE)).thenReturn(secondPage)

    StepVerifier.create(
            controller.questionnaireLeaderboardUpdated(
                QUESTIONNAIRE_ID, CONTENT_ID, LIMIT, user, LOCALE))
        .expectNext(firstPage, secondPage)
        .verifyComplete()
  }

  @Test
  fun shouldRejectAContentOfAnotherQuestionnaireBeforeSubscribing() {
    Mockito.`when`(leaderboardService.requireContentOfQuestionnaire(QUESTIONNAIRE_ID, CONTENT_ID))
        .thenThrow(ContentNotFoundException(CONTENT_ID))
    Assertions.assertThrows(ContentNotFoundException::class.java) {
      controller.questionnaireLeaderboardUpdated(
          QUESTIONNAIRE_ID, CONTENT_ID, LIMIT, User(id = UUID.randomUUID()), LOCALE)
    }
    Mockito.verifyNoInteractions(registry)
  }
}
