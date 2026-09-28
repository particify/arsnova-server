/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.answer

import java.time.Instant
import java.util.Optional
import java.util.UUID
import net.particify.arsnova.core4.questionnaire.content.ChoiceContentFormatData
import net.particify.arsnova.core4.questionnaire.content.Content
import net.particify.arsnova.core4.questionnaire.content.ContentServiceImpl
import net.particify.arsnova.core4.questionnaire.questionnaire.Questionnaire
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnaireRepository
import net.particify.arsnova.core4.questionnaire.round.Round
import net.particify.arsnova.core4.questionnaire.round.RoundRepository
import net.particify.arsnova.core4.questionnaire.round.RoundServiceImpl
import net.particify.arsnova.core4.questionnaire.round.api.RoundStatsEventPublisher
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.data.domain.AuditorAware
import org.springframework.data.domain.ScrollPosition
import org.springframework.data.domain.Window
import org.springframework.security.access.PermissionEvaluator

private const val HALF_OF_DOUBLE_WEIGHT = 500
private const val LEGACY_POINTS = 300
private val OPTIONS = listOf("A", "B", "C")
private val USER_ID = UUID.randomUUID()

// Mockito's matchers return null, which the non-null parameters of a Kotlin interface reject.
private fun eqUserId(): UUID = Mockito.eq(USER_ID) ?: USER_ID

private fun anyUuid(): UUID = Mockito.any(UUID::class.java) ?: USER_ID

class AnswerServiceImplScoringTest {
  private val answerRepository = Mockito.mock(AnswerRepository::class.java)
  private val roundRepository = Mockito.mock(RoundRepository::class.java)
  private val contentServiceImpl = Mockito.mock(ContentServiceImpl::class.java)
  private val questionnaireRepository = Mockito.mock(QuestionnaireRepository::class.java)
  private val answerService =
      AnswerServiceImpl(
          answerRepository,
          roundRepository,
          contentServiceImpl,
          RoundStatsEventPublisher(),
          questionnaireRepository,
          AuditorAware { Optional.of(USER_ID) },
          Mockito.mock(RoundServiceImpl::class.java),
          Mockito.mock(PermissionEvaluator::class.java))
  private val questionnaire = Questionnaire(id = UUID.randomUUID())
  private val ownAnswers = mutableListOf<Answer>()

  init {
    Mockito.`when`(answerRepository.saveAndFlush(Mockito.any(Answer::class.java))).thenAnswer {
      it.arguments[0]
    }
    Mockito.`when`(questionnaireRepository.findById(questionnaire.id!!))
        .thenReturn(Optional.of(questionnaire))
    Mockito.`when`(
            answerRepository.findByRoundIdInAndAuditMetadataCreatedBy(
                Mockito.anyCollection(), eqUserId()))
        .thenAnswer { invocation ->
          val roundIds = invocation.getArgument<Collection<UUID>>(0)
          ownAnswers.filter { it.round?.id in roundIds }
        }
  }

  private fun content(
      weight: Int,
      points: Int? = null,
      answeringExpiredAt: Instant? = null,
      released: Boolean = true,
      withRound: Boolean = true
  ): Content {
    val content =
        Content(
            id = UUID.randomUUID(),
            questionnaire = questionnaire,
            formatData = ChoiceContentFormatData(OPTIONS, listOf(0, 1), true),
            weight = weight,
            points = points)
    if (withRound) {
      val round =
          Round(id = UUID.randomUUID(), content = content, answeringExpiredAt = answeringExpiredAt)
      content.activeRound = round
      Mockito.`when`(roundRepository.findById(round.id!!)).thenReturn(Optional.of(round))
    }
    Mockito.`when`(contentServiceImpl.isPublished(content, questionnaire)).thenReturn(released)
    return content
  }

  private fun answer(content: Content, vararg selectedChoiceIndexes: Int) =
      answerService.createAnswer(
          content.activeRound!!.id!!, ChoiceAnswerFormatData(selectedChoiceIndexes.toList()))

  @Test
  fun shouldStorePartialPointsOfWeightedContentWithWrongResult() {
    val stored = answer(content(weight = 2), 0)
    Assertions.assertEquals(AnswerResultState.WRONG, stored.result)
    Assertions.assertEquals(HALF_OF_DOUBLE_WEIGHT, stored.points)
  }

  @Test
  fun shouldStoreAnswerWhichAchievedNothingWithoutPoints() {
    Assertions.assertNull(answer(content(weight = 1), 0, 2).points)
  }

  @Test
  fun shouldPreferStoredContentPointsOverWeight() {
    Assertions.assertEquals(LEGACY_POINTS, answer(content(2, LEGACY_POINTS), 0, 1).points)
  }

  @Test
  fun shouldSumPartialScoresAndSkipAbstentions() {
    val correct = content(weight = 1, points = Content.BASE_POINTS)
    val partial = content(weight = 2, points = 2 * Content.BASE_POINTS)
    val abstained = content(weight = 1, points = Content.BASE_POINTS)
    givenOwnAnswer(correct, Answer(result = AnswerResultState.CORRECT, formatData = choice(0, 1)))
    givenOwnAnswer(partial, Answer(result = AnswerResultState.WRONG, formatData = choice(1)))
    givenOwnAnswer(abstained, Answer(result = AnswerResultState.ABSTAINED))
    givenContents(correct, partial, abstained)

    val results = answerService.findQuestionnaireAnswerResults(questionnaire.id!!, USER_ID)

    Assertions.assertEquals(1, results.correctAnswerCount)
    Assertions.assertEquals(3, results.scorableContentCount)
    Assertions.assertEquals(Content.BASE_POINTS + HALF_OF_DOUBLE_WEIGHT, results.achievedScore)
    Assertions.assertEquals(4 * Content.BASE_POINTS, results.maxScore)
  }

  @Test
  fun shouldStoreAnswerGivenAfterTheCountdownRanOutWithoutPoints() {
    val expired = content(weight = 1, answeringExpiredAt = Instant.now().minusSeconds(60))
    val stored = answer(expired, 0, 1)
    Assertions.assertEquals(AnswerResultState.CORRECT, stored.result)
    Assertions.assertNull(stored.points)
  }

  @Test
  fun shouldCountButNotScoreAnswerGivenAfterTheCountdownRanOut() {
    val expired =
        content(
            weight = 1,
            points = Content.BASE_POINTS,
            answeringExpiredAt = Instant.now().minusSeconds(60))
    givenOwnAnswer(expired, Answer(result = AnswerResultState.CORRECT, formatData = choice(0, 1)))
    givenContents(expired)

    val results = answerService.findQuestionnaireAnswerResults(questionnaire.id!!, USER_ID)

    Assertions.assertEquals(1, results.correctAnswerCount)
    Assertions.assertEquals(0, results.achievedScore)
    Assertions.assertEquals(Content.BASE_POINTS, results.maxScore)
  }

  @Test
  fun shouldCountContentWithoutActiveRoundAsUnanswered() {
    val answered = content(weight = 1, points = Content.BASE_POINTS)
    val withoutRound = content(weight = 1, points = Content.BASE_POINTS, withRound = false)
    givenOwnAnswer(answered, Answer(result = AnswerResultState.CORRECT, formatData = choice(0, 1)))
    givenContents(answered, withoutRound)

    val results = answerService.findQuestionnaireAnswerResults(questionnaire.id!!, USER_ID)

    Assertions.assertEquals(1, results.correctAnswerCount)
    Assertions.assertEquals(2, results.scorableContentCount)
    Assertions.assertEquals(Content.BASE_POINTS, results.achievedScore)
    Assertions.assertEquals(2 * Content.BASE_POINTS, results.maxScore)
  }

  @Test
  fun shouldLeaveUnreleasedContentsOutOfTheResults() {
    val released = content(weight = 1, points = Content.BASE_POINTS)
    val unreleased = content(weight = 2, points = 2 * Content.BASE_POINTS, released = false)
    givenOwnAnswer(released, Answer(result = AnswerResultState.WRONG, formatData = choice(0)))
    givenOwnAnswer(
        unreleased, Answer(result = AnswerResultState.CORRECT, formatData = choice(0, 1)))
    givenContents(released, unreleased)

    val results = answerService.findQuestionnaireAnswerResults(questionnaire.id!!, USER_ID)

    Assertions.assertEquals(0, results.correctAnswerCount)
    Assertions.assertEquals(1, results.scorableContentCount)
    Assertions.assertEquals(Content.BASE_POINTS, results.maxScore)
  }

  @Test
  fun shouldLoadTheOwnAnswersOfAllContentsInOneQuery() {
    val first = content(weight = 1, points = Content.BASE_POINTS)
    val second = content(weight = 1, points = Content.BASE_POINTS)
    givenOwnAnswer(first, Answer(result = AnswerResultState.CORRECT, formatData = choice(0, 1)))
    givenOwnAnswer(second, Answer(result = AnswerResultState.CORRECT, formatData = choice(0, 1)))
    givenContents(first, second)

    val results = answerService.findQuestionnaireAnswerResults(questionnaire.id!!, USER_ID)

    Assertions.assertEquals(2, results.correctAnswerCount)
    Mockito.verify(answerRepository, Mockito.times(1))
        .findByRoundIdInAndAuditMetadataCreatedBy(Mockito.anyCollection(), eqUserId())
    Mockito.verify(answerRepository, Mockito.never())
        .findByRoundIdAndAuditMetadataCreatedBy(anyUuid(), anyUuid())
  }

  private fun choice(vararg selectedChoiceIndexes: Int) =
      ChoiceAnswerFormatData(selectedChoiceIndexes.toList())

  private fun givenOwnAnswer(content: Content, answer: Answer) {
    answer.round = content.activeRound
    ownAnswers.add(answer)
  }

  private fun givenContents(vararg contents: Content) {
    Mockito.`when`(
            contentServiceImpl.findByQuestionnaireIdOrderByPosition(
                questionnaire.id!!, ScrollPosition.offset()))
        .thenReturn(Window.from(contents.toList()) { ScrollPosition.offset() })
  }
}
