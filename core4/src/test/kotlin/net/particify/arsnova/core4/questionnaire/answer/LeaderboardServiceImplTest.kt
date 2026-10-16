/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.answer

import jakarta.persistence.Tuple
import java.util.Locale
import java.util.Optional
import java.util.UUID
import net.particify.arsnova.core4.questionnaire.content.Content
import net.particify.arsnova.core4.questionnaire.content.ContentRepository
import net.particify.arsnova.core4.questionnaire.exception.ContentNotFoundException
import net.particify.arsnova.core4.questionnaire.exception.QuestionnaireNotFoundException
import net.particify.arsnova.core4.questionnaire.questionnaire.Questionnaire
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnaireRepository
import net.particify.arsnova.core4.questionnaire.round.Round
import net.particify.arsnova.core4.room.RoomUserAlias
import net.particify.arsnova.core4.room.RoomUserAliasProjection
import net.particify.arsnova.core4.room.RoomUserAliasService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.security.access.PermissionEvaluator
import org.springframework.security.authentication.TestingAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder

private val QUESTIONNAIRE_ID = UUID.randomUUID()
private val ROOM_ID = UUID.randomUUID()
private val CONTENT_ID = UUID.randomUUID()
private val ROUND_ID = UUID.randomUUID()
private val LOCALE = Locale.of("en")
private const val ENTRY_COUNT = 1001

class LeaderboardServiceImplTest {
  private val answerRepository = Mockito.mock(AnswerRepository::class.java)
  private val contentRepository = Mockito.mock(ContentRepository::class.java)
  private val questionnaireRepository = Mockito.mock(QuestionnaireRepository::class.java)
  private val roomUserAliasService = Mockito.mock(RoomUserAliasService::class.java)
  private val permissionEvaluator = Mockito.mock(PermissionEvaluator::class.java)
  private val service =
      LeaderboardServiceImpl(
          answerRepository,
          contentRepository,
          questionnaireRepository,
          roomUserAliasService,
          permissionEvaluator)

  @AfterEach
  fun clearAuthentication() {
    SecurityContextHolder.clearContext()
  }

  // Sorted so that position, user ID order and list order agree.
  private val userIds = (1..ENTRY_COUNT).map { UUID.randomUUID() }.sorted()
  private val entries =
      userIds.mapIndexed { index, userId ->
        RawLeaderboardEntry(userId, null, ENTRY_COUNT - index, index + 1, null)
      }

  private fun score(userId: UUID, points: Long?): Tuple {
    val tuple = Mockito.mock(Tuple::class.java)
    Mockito.`when`(tuple.get(0, UUID::class.java)).thenReturn(userId)
    Mockito.`when`(tuple.get(1, Long::class.java)).thenReturn(points)
    return tuple
  }

  private val questionnaire = Questionnaire(id = QUESTIONNAIRE_ID, roomId = ROOM_ID)

  private fun givenQuestionnaire(scores: List<Tuple>) {
    Mockito.`when`(questionnaireRepository.findById(QUESTIONNAIRE_ID))
        .thenReturn(Optional.of(questionnaire))
    Mockito.`when`(answerRepository.sumPointsByQuestionnaireIdGroupByCreatedBy(QUESTIONNAIRE_ID))
        .thenReturn(scores)
  }

  private fun answerBy(userId: UUID, result: AnswerResultState, points: Int?, durationMs: Int?) =
      LeaderboardAnswerProjection(userId, points, durationMs, result)

  @Test
  fun shouldClampTheLimitToZeroUpToTheMaximum() {
    Assertions.assertEquals(0, service.toPage(entries, null, -5, LOCALE, false).entries.size)
    Assertions.assertEquals(
        LeaderboardServiceImpl.MAX_ENTRY_LIMIT,
        service.toPage(entries, null, ENTRY_COUNT, LOCALE, false).entries.size)
    Assertions.assertEquals(
        LeaderboardServiceImpl.DEFAULT_ENTRY_LIMIT,
        service.toPage(entries, null, null, LOCALE, false).entries.size)
  }

  @Test
  fun shouldCountEveryEntryAndKeepTheRankingOrder() {
    val page = service.toPage(entries, null, 3, LOCALE, false)
    Assertions.assertEquals(ENTRY_COUNT, page.totalCount)
    Assertions.assertEquals(listOf(1, 2, 3), page.entries.map { it.position })
  }

  @Test
  fun shouldAddTheOwnEntryOnlyWhenItIsCutFromThePage() {
    val beyond = service.toPage(entries, userIds[5], 3, LOCALE, false)
    Assertions.assertEquals(6, beyond.ownEntry?.position)
    Assertions.assertNull(service.toPage(entries, userIds[2], 3, LOCALE, false).ownEntry)
    Assertions.assertNull(service.toPage(entries, UUID.randomUUID(), 3, LOCALE, false).ownEntry)
    Assertions.assertNull(service.toPage(entries, null, 3, LOCALE, false).ownEntry)
  }

  @Test
  fun shouldLocalizeAliasesForTheReaderAndLeaveMissingOnesEmpty() {
    val projection = RoomUserAliasProjection(userIds[0], UUID.randomUUID(), null, 2)
    val localized = RoomUserAlias(id = projection.id, alias = "Diligent Donkey", seed = 2)
    Mockito.`when`(roomUserAliasService.localize(projection, LOCALE)).thenReturn(localized)
    val withAlias = listOf(entries[0].copy(alias = projection), entries[1])
    val page = service.toPage(withAlias, null, 2, LOCALE, false)
    Assertions.assertSame(localized, page.entries[0].userAlias)
    Assertions.assertNull(page.entries[1].userAlias)
  }

  @Test
  fun shouldRankUsersWithTheirAliasesAndTreatMissingSumsAsZero() {
    val (first, second, third) = userIds.take(3)
    givenQuestionnaire(listOf(score(second, 300), score(first, 900), score(third, null)))
    val alias = RoomUserAliasProjection(first, UUID.randomUUID(), "Captain", null)
    Mockito.`when`(roomUserAliasService.findAliasMappingsByRoomId(ROOM_ID))
        .thenReturn(mapOf(first to alias))

    val ranking = service.findLeaderboardByQuestionnaireId(QUESTIONNAIRE_ID, null)

    Assertions.assertEquals(listOf(first, second, third), ranking.map { it.userId })
    Assertions.assertEquals(listOf(900, 300, 0), ranking.map { it.score })
    Assertions.assertEquals(listOf(alias, null, null), ranking.map { it.alias })
    Assertions.assertTrue(ranking.all { it.currentResult == null })
    Mockito.verifyNoInteractions(contentRepository)
  }

  @Test
  fun shouldAddEachUsersResultForTheActiveRoundOfTheContent() {
    val (first, second) = userIds.take(2)
    givenQuestionnaire(listOf(score(first, 500), score(second, 0)))
    val content =
        Content(id = CONTENT_ID, questionnaire = questionnaire, activeRound = Round(id = ROUND_ID))
    Mockito.`when`(contentRepository.findById(CONTENT_ID)).thenReturn(Optional.of(content))
    Mockito.`when`(answerRepository.findLeaderboardProjectionsByRoundId(ROUND_ID))
        .thenReturn(
            listOf(
                answerBy(first, AnswerResultState.CORRECT, 500, 1200),
                answerBy(second, AnswerResultState.WRONG, null, null)))

    val ranking = service.findLeaderboardByQuestionnaireId(QUESTIONNAIRE_ID, CONTENT_ID)

    Assertions.assertEquals(
        listOf(LeaderboardCurrentResult(500, 1200, true), LeaderboardCurrentResult(0, 0, false)),
        ranking.map { it.currentResult })
  }

  @Test
  fun shouldOmitResultsForAContentWithoutActiveRound() {
    givenQuestionnaire(listOf(score(userIds[0], 500)))
    Mockito.`when`(contentRepository.findById(CONTENT_ID))
        .thenReturn(Optional.of(Content(id = CONTENT_ID, questionnaire = questionnaire)))
    val ranking = service.findLeaderboardByQuestionnaireId(QUESTIONNAIRE_ID, CONTENT_ID)
    Assertions.assertNull(ranking.single().currentResult)
  }

  private fun scoredEntries(solutionPublished: Boolean) =
      listOf(
          entries[0].copy(
              currentResult = LeaderboardCurrentResult(500, 1200, true),
              solutionPublished = solutionPublished),
          entries[1].copy(
              currentResult = LeaderboardCurrentResult(0, 900, false),
              solutionPublished = solutionPublished))

  /** The reader's own entry is cut from a page of one, so it is withheld along with the others. */
  private fun currentResultsOnPage(
      solutionPublished: Boolean,
      maySeeUnpublishedResults: Boolean
  ): List<LeaderboardCurrentResult?> {
    val page =
        service.toPage(
            scoredEntries(solutionPublished), userIds[1], 1, LOCALE, maySeeUnpublishedResults)
    return (page.entries + page.ownEntry!!).map { it.currentResult }
  }

  @Test
  fun shouldWithholdCurrentResultsFromParticipantBeforeTheSolutionIsPublished() {
    Assertions.assertEquals(
        listOf(LeaderboardCurrentResult(0, 1200, false), LeaderboardCurrentResult(0, 900, false)),
        currentResultsOnPage(solutionPublished = false, maySeeUnpublishedResults = false))
  }

  @Test
  fun shouldShowCurrentResultsToEditorBeforeTheSolutionIsPublished() {
    Assertions.assertEquals(
        listOf(LeaderboardCurrentResult(500, 1200, true), LeaderboardCurrentResult(0, 900, false)),
        currentResultsOnPage(solutionPublished = false, maySeeUnpublishedResults = true))
  }

  @Test
  fun shouldShowCurrentResultsToEveryoneOnceTheSolutionIsPublished() {
    val results =
        listOf(LeaderboardCurrentResult(500, 1200, true), LeaderboardCurrentResult(0, 900, false))
    Assertions.assertEquals(
        results, currentResultsOnPage(solutionPublished = true, maySeeUnpublishedResults = false))
    Assertions.assertEquals(
        results, currentResultsOnPage(solutionPublished = true, maySeeUnpublishedResults = true))
  }

  @Test
  fun shouldTellWhetherTheSolutionOfTheRankedQuestionnaireIsPublished() {
    givenQuestionnaire(listOf(score(userIds[0], 500)))
    questionnaire.correctOptionsPublished = false
    Assertions.assertFalse(
        service.findLeaderboardByQuestionnaireId(QUESTIONNAIRE_ID, null).single().solutionPublished)
    questionnaire.correctOptionsPublished = true
    Assertions.assertTrue(
        service.findLeaderboardByQuestionnaireId(QUESTIONNAIRE_ID, null).single().solutionPublished)
  }

  private fun givenReaderMayEditContent(mayEdit: Boolean) {
    val reader = TestingAuthenticationToken("user", null, "ROLE_USER")
    SecurityContextHolder.getContext().authentication = reader
    Mockito.`when`(permissionEvaluator.hasPermission(reader, CONTENT_ID, "Content", "write"))
        .thenReturn(mayEdit)
    Mockito.`when`(contentRepository.findById(CONTENT_ID))
        .thenReturn(Optional.of(Content(id = CONTENT_ID, questionnaire = questionnaire)))
  }

  @Test
  fun shouldLetOnlyPrivilegedReadersSeeUnpublishedResults() {
    givenReaderMayEditContent(true)
    Assertions.assertTrue(service.maySeeUnpublishedResults(QUESTIONNAIRE_ID, CONTENT_ID))
    givenReaderMayEditContent(false)
    Assertions.assertFalse(service.maySeeUnpublishedResults(QUESTIONNAIRE_ID, CONTENT_ID))
    Assertions.assertFalse(service.maySeeUnpublishedResults(QUESTIONNAIRE_ID, null))
  }

  @Test
  fun shouldRejectUnknownQuestionnaireOrContent() {
    Assertions.assertThrows(QuestionnaireNotFoundException::class.java) {
      service.findLeaderboardByQuestionnaireId(QUESTIONNAIRE_ID, null)
    }
    givenQuestionnaire(listOf())
    Assertions.assertThrows(ContentNotFoundException::class.java) {
      service.findLeaderboardByQuestionnaireId(QUESTIONNAIRE_ID, CONTENT_ID)
    }
  }

  @Test
  fun shouldRejectAContentOfAnotherQuestionnaireLikeAnUnknownOne() {
    givenQuestionnaire(listOf(score(userIds[0], 500)))
    val foreign = Questionnaire(id = UUID.randomUUID(), roomId = UUID.randomUUID())
    val content =
        Content(id = CONTENT_ID, questionnaire = foreign, activeRound = Round(id = ROUND_ID))
    Mockito.`when`(contentRepository.findById(CONTENT_ID)).thenReturn(Optional.of(content))
    Assertions.assertThrows(ContentNotFoundException::class.java) {
      service.findLeaderboardByQuestionnaireId(QUESTIONNAIRE_ID, CONTENT_ID)
    }
    Mockito.verify(answerRepository, Mockito.never()).findLeaderboardProjectionsByRoundId(ROUND_ID)
  }
}
