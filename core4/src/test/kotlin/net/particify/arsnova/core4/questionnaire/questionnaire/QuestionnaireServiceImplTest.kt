/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.questionnaire

import java.time.Instant
import java.util.Optional
import java.util.UUID
import net.particify.arsnova.core4.questionnaire.content.Content
import net.particify.arsnova.core4.questionnaire.content.ContentRepository
import net.particify.arsnova.core4.questionnaire.content.ContentServiceImpl
import net.particify.arsnova.core4.questionnaire.questionnaire.api.QuestionnaireEventPublisher
import net.particify.arsnova.core4.questionnaire.round.Round
import net.particify.arsnova.core4.questionnaire.round.RoundServiceImpl
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.context.ApplicationEventPublisher

private val QUESTIONNAIRE_ID = UUID.randomUUID()

class QuestionnaireServiceImplTest {
  private val questionnaireRepository = Mockito.mock(QuestionnaireRepository::class.java)
  private val questionnaireService =
      QuestionnaireServiceImpl(
          questionnaireRepository,
          Mockito.mock(ApplicationEventPublisher::class.java),
          Mockito.mock(ContentRepository::class.java),
          Mockito.mock(RoundServiceImpl::class.java),
          QuestionnaireEventPublisher(),
          Mockito.mock(ContentServiceImpl::class.java))
  private val first = Content(id = UUID.randomUUID(), activeRound = Round())
  private val second = Content(id = UUID.randomUUID(), activeRound = Round())
  private val third = Content(id = UUID.randomUUID(), activeRound = Round())

  private fun givenQuestionnaire(mode: QuestionnairePublishingMode): Questionnaire {
    val questionnaire =
        Questionnaire(
            id = QUESTIONNAIRE_ID,
            roomId = UUID.randomUUID(),
            publishingMode = mode,
            contents = mutableListOf(first, second, third))
    Mockito.`when`(questionnaireRepository.findById(QUESTIONNAIRE_ID))
        .thenReturn(Optional.of(questionnaire))
    Mockito.`when`(questionnaireRepository.save(questionnaire)).thenReturn(questionnaire)
    return questionnaire
  }

  private fun changeMode(mode: QuestionnairePublishingMode) =
      questionnaireService.update(QUESTIONNAIRE_ID, null, null, mode, null, null, null)

  @Test
  fun shouldReleaseOnlyTheFirstContentWhenLeavingAllMode() {
    listOf(QuestionnairePublishingMode.UP_TO, QuestionnairePublishingMode.LIVE).forEach {
      givenQuestionnaire(QuestionnairePublishingMode.ALL)
      val updated = changeMode(it)
      Assertions.assertEquals(it, updated.publishingMode)
      Assertions.assertSame(first, updated.publishedUpToContent)
    }
  }

  @Test
  fun shouldKeepReleasedRangeWhenModeIsResent() {
    givenQuestionnaire(QuestionnairePublishingMode.UP_TO).publishedUpToContent = second
    Assertions.assertSame(
        second, changeMode(QuestionnairePublishingMode.UP_TO).publishedUpToContent)
  }

  @Test
  fun shouldOpenEveryRoundWithoutCountdownWhenLeavingLiveMode() {
    val now = Instant.now()
    listOf(first, second, third).forEach {
      it.activeRound!!.startAnswering(60, now)
      it.activeRound!!.stopAnswering(now)
    }
    givenQuestionnaire(QuestionnairePublishingMode.LIVE)
    changeMode(QuestionnairePublishingMode.ALL)
    listOf(first, second, third).forEach {
      val round = it.activeRound!!
      Assertions.assertFalse(round.closed)
      Assertions.assertNull(round.answeringEndTime)
      Assertions.assertNull(round.answeringPausedAt)
    }
  }

  @Test
  fun shouldLeaveRoundsAloneWhenSwitchingBetweenOtherModes() {
    val round = first.activeRound!!
    round.stopAnswering()
    givenQuestionnaire(QuestionnairePublishingMode.UP_TO)
    changeMode(QuestionnairePublishingMode.LIVE)
    Assertions.assertTrue(round.closed)
  }

  @Test
  fun shouldOrderContentsAsListed() {
    givenQuestionnaire(QuestionnairePublishingMode.ALL)
    val updated =
        questionnaireService.updateContentOrder(
            QUESTIONNAIRE_ID, listOf(third.id!!, first.id!!, second.id!!))
    Assertions.assertEquals(listOf(third, first, second), updated.contents)
  }

  @Test
  fun shouldKeepUnlistedContentsInTheirOrderAfterListedOnes() {
    givenQuestionnaire(QuestionnairePublishingMode.ALL)
    val updated = questionnaireService.updateContentOrder(QUESTIONNAIRE_ID, listOf(third.id!!))
    Assertions.assertEquals(listOf(third, first, second), updated.contents)
  }
}
