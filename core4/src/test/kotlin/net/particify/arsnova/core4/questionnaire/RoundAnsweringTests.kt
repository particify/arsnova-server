/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire

import jakarta.validation.ConstraintViolationException
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import net.particify.arsnova.core4.TestcontainersConfiguration
import net.particify.arsnova.core4.questionnaire.answer.Answer
import net.particify.arsnova.core4.questionnaire.answer.AnswerRepository
import net.particify.arsnova.core4.questionnaire.answer.AnswerResultState
import net.particify.arsnova.core4.questionnaire.answer.AnswerServiceImpl
import net.particify.arsnova.core4.questionnaire.answer.ChoiceAnswerFormatData
import net.particify.arsnova.core4.questionnaire.answer.WordcloudAnswerFormatData
import net.particify.arsnova.core4.questionnaire.content.ChoiceContentFormatData
import net.particify.arsnova.core4.questionnaire.content.Content
import net.particify.arsnova.core4.questionnaire.content.ContentServiceImpl
import net.particify.arsnova.core4.questionnaire.content.WordcloudContentFormatData
import net.particify.arsnova.core4.questionnaire.exception.AbstentionsNotAllowedException
import net.particify.arsnova.core4.questionnaire.exception.AnswerAlreadyExistsException
import net.particify.arsnova.core4.questionnaire.exception.RoundClosedException
import net.particify.arsnova.core4.questionnaire.exception.RoundExpiredException
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnairePublishingMode
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnairePublishingServiceImpl
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnaireServiceImpl
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnaireType
import net.particify.arsnova.core4.questionnaire.round.Round
import net.particify.arsnova.core4.questionnaire.round.RoundServiceImpl
import net.particify.arsnova.core4.questionnaire.round.api.AnswerChangedEvent
import net.particify.arsnova.core4.questionnaire.round.api.RoundQueryController
import net.particify.arsnova.core4.questionnaire.round.api.RoundStatsEventPublisher
import net.particify.arsnova.core4.user.UserService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.ActiveProfiles

/** Accounts and rooms from the `dev` Liquibase fixtures. */
private val USER_ID = UUID.fromString("9c778494-9b52-424d-8c7f-f4936629facb")
private val PARTICIPANT_ID = UUID.fromString("43389844-708b-469d-8d22-abdd2eb88777")

/** Owned by [USER_ID]; [PARTICIPANT_ID] is a participant. */
private val ROOM_ID = UUID.fromString("3f9cecb5-6dbf-45cc-b630-4af700d474b6")
private const val DURATION = 30

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration::class)
class RoundAnsweringTests {
  @Autowired lateinit var answerRepository: AnswerRepository
  @Autowired lateinit var answerService: AnswerServiceImpl
  @Autowired lateinit var contentService: ContentServiceImpl
  @Autowired lateinit var publishingService: QuestionnairePublishingServiceImpl
  @Autowired lateinit var questionnaireService: QuestionnaireServiceImpl
  @Autowired lateinit var roundQueryController: RoundQueryController
  @Autowired lateinit var roundService: RoundServiceImpl
  @Autowired lateinit var roundStatsEventPublisher: RoundStatsEventPublisher
  @Autowired lateinit var userService: UserService

  @BeforeEach
  fun authenticateUser() {
    authenticate(USER_ID)
  }

  private fun authenticate(userId: UUID) {
    val user = userService.loadUserById(userId)!!
    SecurityContextHolder.getContext().authentication =
        UsernamePasswordAuthenticationToken(user, null, user.authorities)
  }

  @AfterEach
  fun clearAuthentication() {
    SecurityContextHolder.clearContext()
  }

  private fun createQuestionnaire(type: QuestionnaireType) =
      questionnaireService.create(ROOM_ID, "Countdown ${UUID.randomUUID()}", type)

  private fun createChoiceContent(
      questionnaireId: UUID,
      duration: Int?,
      abstentionsAllowed: Boolean = true
  ): Content =
      contentService.create(
          questionnaireId,
          "Choice content",
          ChoiceContentFormatData(listOf("A", "B"), listOf(0), false),
          duration,
          abstentionsAllowed,
          null)

  /**
   * The events for [roundId] published while [action] runs. The publisher emits on the calling
   * thread, but replays events buffered while nobody was subscribed, hence the filter.
   */
  private fun answerChangedEventsDuring(
      roundId: UUID,
      action: () -> Unit
  ): List<AnswerChangedEvent> {
    val events = mutableListOf<AnswerChangedEvent>()
    val subscription = roundStatsEventPublisher.answerChangedFlux().subscribe { events.add(it) }
    try {
      action()
    } finally {
      subscription.dispose()
    }
    return events.filter { it.roundId == roundId }
  }

  private fun createWordcloudContent(questionnaireId: UUID, duration: Int?): Content =
      contentService.create(
          questionnaireId, "Wordcloud content", WordcloudContentFormatData(3), duration, true, null)

  private fun startLiveContent(contentId: UUID): Round =
      publishingService.startLiveContent(contentId).activeRound!!

  private fun expire(round: Round): Round {
    round.answeringEndTime = Instant.now().minus(1, ChronoUnit.MINUTES)
    return roundService.save(round)
  }

  private fun reload(round: Round): Round = roundService.findById(round.id!!).orElseThrow()

  private fun constraintViolationOf(throwable: Throwable) =
      generateSequence(throwable) { it.cause }
          .filterIsInstance<ConstraintViolationException>()
          .firstOrNull()

  @Test
  fun shouldRejectDurationAboveMaximum() {
    val questionnaire = createQuestionnaire(QuestionnaireType.QUIZ)
    val thrown =
        Assertions.assertThrows(RuntimeException::class.java) {
          createChoiceContent(questionnaire.id!!, (Content.MAX_DURATION + 1).toInt())
        }
    val violation = constraintViolationOf(thrown)
    Assertions.assertNotNull(violation)
    Assertions.assertEquals(
        "duration", violation!!.constraintViolations.first().propertyPath.toString())
  }

  @Test
  fun shouldClearDurationWhenUpdatedWithNull() {
    val questionnaire = createQuestionnaire(QuestionnaireType.QUIZ)
    val content = createChoiceContent(questionnaire.id!!, DURATION)
    Assertions.assertEquals(DURATION, content.duration)
    val updated =
        contentService.update(
            content.id!!,
            "Choice content",
            ChoiceContentFormatData(listOf("A", "B"), listOf(0), false),
            null,
            true,
            null)
    Assertions.assertNull(updated.duration)
    Assertions.assertNull(contentService.findById(content.id!!).orElseThrow().duration)
    Assertions.assertNull(startLiveContent(content.id!!).answeringEndTime)
  }

  @Test
  fun shouldAcceptDurationAtMaximum() {
    val questionnaire = createQuestionnaire(QuestionnaireType.QUIZ)
    val content = createChoiceContent(questionnaire.id!!, Content.MAX_DURATION.toInt())
    Assertions.assertEquals(Content.MAX_DURATION.toInt(), content.duration)
  }

  @Test
  fun shouldStampDeadlineWhenStartingLiveContentWithDuration() {
    val questionnaire = createQuestionnaire(QuestionnaireType.QUIZ)
    val content = createChoiceContent(questionnaire.id!!, DURATION)
    val before = Instant.now()
    val round = startLiveContent(content.id!!)
    Assertions.assertNotNull(round.answeringEndTime)
    Assertions.assertFalse(round.answeringEndTime!!.isBefore(before.plusSeconds(DURATION.toLong())))
    Assertions.assertFalse(round.closed)
    Assertions.assertTrue(round.acceptsAnswers())
  }

  @Test
  fun shouldNotStampDeadlineWhenStartingLiveContentWithoutDuration() {
    val questionnaire = createQuestionnaire(QuestionnaireType.QUIZ)
    val content = createChoiceContent(questionnaire.id!!, null)
    val round = startLiveContent(content.id!!)
    Assertions.assertNull(round.answeringEndTime)
    Assertions.assertTrue(round.acceptsAnswers())
  }

  @Test
  fun shouldNotStampDeadlineWhenStartingLiveContentWithZeroDuration() {
    val questionnaire = createQuestionnaire(QuestionnaireType.QUIZ)
    val content = createChoiceContent(questionnaire.id!!, 0)
    val round = startLiveContent(content.id!!)
    Assertions.assertNull(round.answeringEndTime)
    Assertions.assertTrue(round.acceptsAnswers())
  }

  @Test
  fun shouldResumeWithRemainingTimeWhenRestartingLiveContent() {
    val questionnaire = createQuestionnaire(QuestionnaireType.QUIZ)
    val content = createChoiceContent(questionnaire.id!!, DURATION)
    val firstRound = startLiveContent(content.id!!)
    val firstEndTime = firstRound.answeringEndTime!!
    Thread.sleep(500) // let a meaningful chunk of the countdown elapse before stopping
    val beforeStop = Instant.now()
    publishingService.stopLiveContent(questionnaire.id!!)
    val afterStop = Instant.now()
    val beforeReopen = Instant.now()
    val restartedRound = startLiveContent(content.id!!)
    val afterReopen = Instant.now()

    Assertions.assertEquals(firstRound.id, restartedRound.id)
    Assertions.assertFalse(restartedRound.closed)
    Assertions.assertNotNull(restartedRound.answeringEndTime)

    // Resumed deadline ~= reopenTime + (firstEndTime - stopTime), i.e. the leftover countdown,
    // not `reopenTime + DURATION`.
    val minExpected = beforeReopen.plus(Duration.between(afterStop, firstEndTime))
    val maxExpected = afterReopen.plus(Duration.between(beforeStop, firstEndTime))
    Assertions.assertFalse(restartedRound.answeringEndTime!!.isBefore(minExpected))
    Assertions.assertFalse(restartedRound.answeringEndTime!!.isAfter(maxExpected))

    // Strictly short of a fresh full duration, unlike the old re-stamping behavior.
    Assertions.assertTrue(
        restartedRound.answeringEndTime!!.isBefore(beforeReopen.plusSeconds(DURATION.toLong())))
  }

  @Test
  fun shouldStampDeadlineForNewRoundOfLiveQuestionnaire() {
    val questionnaire = createQuestionnaire(QuestionnaireType.QUIZ)
    val content = createChoiceContent(questionnaire.id!!, DURATION)
    val firstRound = startLiveContent(content.id!!)
    Thread.sleep(500) // let a chunk of the first round's countdown elapse before it stops early
    publishingService.stopLiveContent(questionnaire.id!!)
    val before = Instant.now()
    val secondRound = roundService.startNewRound(content.id!!)
    Assertions.assertEquals(2, secondRound.roundNo)
    Assertions.assertNotNull(secondRound.answeringEndTime)
    // The escape hatch always gives a fresh full duration, unlike reopening the same round.
    Assertions.assertFalse(
        secondRound.answeringEndTime!!.isBefore(before.plusSeconds(DURATION.toLong())))
    Assertions.assertTrue(secondRound.acceptsAnswers())
    Assertions.assertTrue(reload(firstRound).closed)
  }

  @Test
  fun shouldNotStampDeadlineForNewRoundOfNonLiveQuestionnaire() {
    val questionnaire = createQuestionnaire(QuestionnaireType.SURVEY)
    Assertions.assertEquals(QuestionnairePublishingMode.ALL, questionnaire.publishingMode)
    val content = createChoiceContent(questionnaire.id!!, DURATION)
    val secondRound = roundService.startNewRound(content.id!!)
    Assertions.assertEquals(2, secondRound.roundNo)
    Assertions.assertNull(secondRound.answeringEndTime)
  }

  @Test
  fun shouldKeepOtherContentsOpenWhenStartingNewRoundOutsideLiveMode() {
    val questionnaire = createQuestionnaire(QuestionnaireType.SURVEY)
    val first = createChoiceContent(questionnaire.id!!, null)
    val second = createChoiceContent(questionnaire.id!!, null)
    val firstRound = roundService.startNewRound(first.id!!)
    roundService.startNewRound(second.id!!)
    Assertions.assertFalse(reload(firstRound).closed)
    val answer = answerService.createAnswer(firstRound.id!!, ChoiceAnswerFormatData(listOf(0)))
    Assertions.assertNotNull(answer.id)
  }

  @Test
  fun shouldNotMovePublishedRangeWhenStartingNewRoundOutsideLiveMode() {
    val questionnaire = createQuestionnaire(QuestionnaireType.SURVEY)
    val first = createChoiceContent(questionnaire.id!!, null)
    val second = createChoiceContent(questionnaire.id!!, null)
    questionnaireService.update(
        questionnaire.id!!, null, null, QuestionnairePublishingMode.UP_TO, null, null, null)
    roundService.startNewRound(second.id!!)
    val reloaded = questionnaireService.findById(questionnaire.id!!).orElseThrow()
    Assertions.assertEquals(first.id, reloaded.publishedUpToContent?.id)
    Assertions.assertNull(reloaded.activeRound)
  }

  @Test
  fun shouldKeepDeadlineWhenStoppingLiveContent() {
    val questionnaire = createQuestionnaire(QuestionnaireType.QUIZ)
    val content = createChoiceContent(questionnaire.id!!, DURATION)
    val round = startLiveContent(content.id!!)
    val endTime = round.answeringEndTime!!
    Assertions.assertNull(round.answeringPausedAt)
    val beforeStop = Instant.now()
    publishingService.stopLiveContent(questionnaire.id!!)
    val afterStop = Instant.now()
    val stoppedRound = reload(round)
    Assertions.assertTrue(stoppedRound.closed)
    Assertions.assertEquals(
        endTime.truncatedTo(ChronoUnit.MILLIS),
        stoppedRound.answeringEndTime!!.truncatedTo(ChronoUnit.MILLIS))
    Assertions.assertNotNull(stoppedRound.answeringPausedAt)
    Assertions.assertFalse(stoppedRound.answeringPausedAt!!.isBefore(beforeStop))
    Assertions.assertFalse(stoppedRound.answeringPausedAt!!.isAfter(afterStop))
    Assertions.assertFalse(stoppedRound.acceptsAnswers())
    Assertions.assertThrows(RoundClosedException::class.java) {
      answerService.createAnswer(round.id!!, ChoiceAnswerFormatData(listOf(0)))
    }
  }

  @Test
  fun shouldRejectAbstentionWhenContentDoesNotAllowIt() {
    val questionnaire = createQuestionnaire(QuestionnaireType.SURVEY)
    val round =
        createChoiceContent(questionnaire.id!!, null, abstentionsAllowed = false).activeRound!!
    Assertions.assertThrows(AbstentionsNotAllowedException::class.java) {
      answerService.createAbstention(round.id!!)
    }
    Assertions.assertTrue(answerRepository.findByRoundId(round.id!!).isEmpty())
  }

  @Test
  fun shouldPublishAbstentionLikeAnAnswer() {
    val questionnaire = createQuestionnaire(QuestionnaireType.SURVEY)
    val content = createChoiceContent(questionnaire.id!!, null)
    val roundId = content.activeRound!!.id!!
    val events = answerChangedEventsDuring(roundId) { answerService.createAbstention(roundId) }
    Assertions.assertEquals(
        listOf(AnswerChangedEvent(roundId, content.id!!, questionnaire.id!!)), events)
  }

  @Test
  fun shouldStartOverWithClosedFirstRoundWhenDeletingAnswersInLiveMode() {
    val questionnaire = createQuestionnaire(QuestionnaireType.QUIZ)
    val content = createChoiceContent(questionnaire.id!!, DURATION)
    val firstRound = startLiveContent(content.id!!)
    answerService.createAnswer(firstRound.id!!, ChoiceAnswerFormatData(listOf(0)))
    val secondRound = roundService.startNewRound(content.id!!)
    answerService.createAnswer(secondRound.id!!, ChoiceAnswerFormatData(listOf(1)))

    answerService.deleteByContentId(content.id!!)

    val rounds = roundService.findByContentIdOrderByRoundNo(content.id!!)
    val newRound = rounds.single()
    Assertions.assertEquals(1, newRound.roundNo)
    Assertions.assertTrue(newRound.closed)
    Assertions.assertNull(newRound.answeringEndTime)
    val oldRoundIds = listOf(firstRound.id!!, secondRound.id!!)
    Assertions.assertFalse(newRound.id in oldRoundIds)
    Assertions.assertEquals(
        newRound.id, contentService.findById(content.id!!).orElseThrow().activeRound?.id)
    Assertions.assertTrue(roundService.findAllById(oldRoundIds).isEmpty())
    Assertions.assertTrue(answerRepository.findByRoundId(newRound.id!!).isEmpty())
    Assertions.assertNull(
        questionnaireService.findById(questionnaire.id!!).orElseThrow().activeRound)
  }

  @Test
  fun shouldStartOverWithOpenFirstRoundWhenDeletingAnswersOutsideLiveMode() {
    val questionnaire = createQuestionnaire(QuestionnaireType.SURVEY)
    val content = createChoiceContent(questionnaire.id!!, null)
    answerService.createAnswer(content.activeRound!!.id!!, ChoiceAnswerFormatData(listOf(0)))
    answerService.deleteByContentId(content.id!!)
    val newRound = roundService.findByContentIdOrderByRoundNo(content.id!!).single()
    Assertions.assertFalse(newRound.closed)
    val answer = answerService.createAnswer(newRound.id!!, ChoiceAnswerFormatData(listOf(0)))
    Assertions.assertNotNull(answer.id)
  }

  @Test
  fun shouldSignalTheFreshRoundWhenDeletingAnswers() {
    val questionnaire = createQuestionnaire(QuestionnaireType.SURVEY)
    val content = createChoiceContent(questionnaire.id!!, null)
    val events = mutableListOf<AnswerChangedEvent>()
    val subscription = roundStatsEventPublisher.answerChangedFlux().subscribe { events.add(it) }
    try {
      answerService.deleteByContentId(content.id!!)
      questionnaireService.resetAnswerResultsByQuestionnaireId(questionnaire.id!!)
    } finally {
      subscription.dispose()
    }
    val roundIdsSignalled = events.filter { it.contentId == content.id }.map { it.roundId }
    Assertions.assertEquals(2, roundIdsSignalled.size)
    Assertions.assertEquals(
        roundService.findByContentIdOrderByRoundNo(content.id!!).single().id,
        roundIdsSignalled.last())
  }

  @Test
  fun shouldRejectSecondAnswerOfSameUserInRound() {
    val questionnaire = createQuestionnaire(QuestionnaireType.SURVEY)
    val round = createChoiceContent(questionnaire.id!!, null).activeRound!!
    answerService.createAnswer(round.id!!, ChoiceAnswerFormatData(listOf(0)))
    Assertions.assertThrows(AnswerAlreadyExistsException::class.java) {
      answerService.createAnswer(round.id!!, ChoiceAnswerFormatData(listOf(1)))
    }
    Assertions.assertThrows(AnswerAlreadyExistsException::class.java) {
      answerService.createAbstention(round.id!!)
    }
    Assertions.assertEquals(1, answerRepository.findByRoundId(round.id!!).size)
  }

  @Test
  fun shouldRejectAnswerAfterAbstentionInRound() {
    val questionnaire = createQuestionnaire(QuestionnaireType.SURVEY)
    val round = createChoiceContent(questionnaire.id!!, null).activeRound!!
    answerService.createAbstention(round.id!!)
    Assertions.assertThrows(AnswerAlreadyExistsException::class.java) {
      answerService.createAnswer(round.id!!, ChoiceAnswerFormatData(listOf(0)))
    }
    Assertions.assertThrows(AnswerAlreadyExistsException::class.java) {
      answerService.createAbstention(round.id!!)
    }
  }

  @Test
  fun shouldAcceptOneAnswerPerUserInRound() {
    val questionnaire = createQuestionnaire(QuestionnaireType.SURVEY)
    val content = createChoiceContent(questionnaire.id!!, null)
    val round = content.activeRound!!
    answerService.createAnswer(round.id!!, ChoiceAnswerFormatData(listOf(0)))
    authenticate(PARTICIPANT_ID)
    answerService.createAnswer(round.id!!, ChoiceAnswerFormatData(listOf(1)))
    val secondRound = roundService.startNewRound(content.id!!)
    answerService.createAnswer(secondRound.id!!, ChoiceAnswerFormatData(listOf(0)))
    Assertions.assertEquals(2, answerRepository.findByRoundId(round.id!!).size)
    Assertions.assertEquals(1, answerRepository.findByRoundId(secondRound.id!!).size)
  }

  @Test
  fun shouldEnforceOneAnswerPerUserAndRoundInTheDatabase() {
    val questionnaire = createQuestionnaire(QuestionnaireType.SURVEY)
    val round = createChoiceContent(questionnaire.id!!, null).activeRound!!
    answerRepository.saveAndFlush(Answer(round = round, result = AnswerResultState.NEUTRAL))
    Assertions.assertThrows(DataIntegrityViolationException::class.java) {
      answerRepository.saveAndFlush(Answer(round = round, result = AnswerResultState.ABSTAINED))
    }
  }

  @Test
  fun shouldRejectChoiceAnswerAfterDeadline() {
    val questionnaire = createQuestionnaire(QuestionnaireType.QUIZ)
    val content = createChoiceContent(questionnaire.id!!, DURATION)
    val round = expire(startLiveContent(content.id!!))
    Assertions.assertThrows(RoundExpiredException::class.java) {
      answerService.createAnswer(round.id!!, ChoiceAnswerFormatData(listOf(0)))
    }
  }

  @Test
  fun shouldRejectWordcloudAnswerAfterDeadline() {
    val questionnaire = createQuestionnaire(QuestionnaireType.QUIZ)
    val content = createWordcloudContent(questionnaire.id!!, DURATION)
    val round = expire(startLiveContent(content.id!!))
    Assertions.assertThrows(RoundExpiredException::class.java) {
      answerService.createAnswer(round.id!!, WordcloudAnswerFormatData(listOf("word")))
    }
  }

  @Test
  fun shouldRejectAbstentionAfterDeadline() {
    val questionnaire = createQuestionnaire(QuestionnaireType.QUIZ)
    val content = createChoiceContent(questionnaire.id!!, DURATION)
    val round = expire(startLiveContent(content.id!!))
    Assertions.assertThrows(RoundExpiredException::class.java) {
      answerService.createAbstention(round.id!!)
    }
  }

  @Test
  fun shouldAcceptChoiceAnswerBeforeDeadline() {
    val questionnaire = createQuestionnaire(QuestionnaireType.QUIZ)
    val content = createChoiceContent(questionnaire.id!!, DURATION)
    val round = startLiveContent(content.id!!)
    val answer = answerService.createAnswer(round.id!!, ChoiceAnswerFormatData(listOf(0)))
    Assertions.assertNotNull(answer.id)
    Assertions.assertNotNull(
        answerService.findByRoundIdAndAuditMetadataCreatedBy(round.id!!, USER_ID))
  }

  @Test
  fun shouldClearDeadlineWhenSwitchingAwayFromLivePublishingMode() {
    val questionnaire = createQuestionnaire(QuestionnaireType.QUIZ)
    val content = createChoiceContent(questionnaire.id!!, DURATION)
    val round = startLiveContent(content.id!!)
    Assertions.assertNotNull(round.answeringEndTime)
    publishingService.stopLiveContent(questionnaire.id!!)
    Assertions.assertNotNull(reload(round).answeringPausedAt)
    questionnaireService.update(
        questionnaire.id!!, null, null, QuestionnairePublishingMode.ALL, null, null, null)
    val reset = reload(round)
    Assertions.assertFalse(reset.closed)
    Assertions.assertNull(reset.answeringEndTime)
    Assertions.assertNull(reset.answeringPausedAt)
  }

  @Test
  fun shouldAcceptAnswersForContentsCreatedInLiveModeAfterLeavingIt() {
    val questionnaire = createQuestionnaire(QuestionnaireType.QUIZ)
    createChoiceContent(questionnaire.id!!, DURATION)
    val neverPresented = createChoiceContent(questionnaire.id!!, DURATION)
    val round = neverPresented.activeRound!!
    Assertions.assertTrue(reload(round).closed)
    questionnaireService.update(
        questionnaire.id!!, null, null, QuestionnairePublishingMode.ALL, null, null, null)
    Assertions.assertFalse(reload(round).closed)
    val answer = answerService.createAnswer(round.id!!, ChoiceAnswerFormatData(listOf(0)))
    Assertions.assertNotNull(answer.id)
  }

  @Test
  fun shouldNotRewindPublishedRangeWhenResendingPublishingMode() {
    val questionnaire = createQuestionnaire(QuestionnaireType.QUIZ)
    createChoiceContent(questionnaire.id!!, DURATION)
    val second = createChoiceContent(questionnaire.id!!, DURATION)
    publishingService.publishUpToContent(questionnaire.id!!, second.id!!)
    questionnaireService.update(
        questionnaire.id!!, null, null, QuestionnairePublishingMode.LIVE, null, null, null)
    val reloaded = questionnaireService.findById(questionnaire.id!!).orElseThrow()
    Assertions.assertEquals(second.id, reloaded.publishedUpToContent?.id)
  }

  @Test
  fun shouldStartAFreshCountdownWhenReopenedAfterBeingStoppedPastExpiry() {
    val questionnaire = createQuestionnaire(QuestionnaireType.QUIZ)
    val content = createChoiceContent(questionnaire.id!!, DURATION)
    val round = expire(startLiveContent(content.id!!))
    publishingService.stopLiveContent(questionnaire.id!!)
    val beforeReopen = Instant.now()
    val reopened = startLiveContent(content.id!!)
    val afterReopen = Instant.now()
    Assertions.assertEquals(round.id, reopened.id)
    Assertions.assertFalse(reopened.closed)
    val endTime = reopened.answeringEndTime!!
    Assertions.assertFalse(endTime.isBefore(beforeReopen.plusSeconds(DURATION.toLong())))
    Assertions.assertFalse(endTime.isAfter(afterReopen.plusSeconds(DURATION.toLong())))
    Assertions.assertTrue(reopened.acceptsAnswers())
    val answer = answerService.createAnswer(reopened.id!!, ChoiceAnswerFormatData(listOf(0)))
    Assertions.assertNotNull(answer.id)
  }

  @Test
  fun shouldStayUntimedWhenReopeningUntimedContent() {
    val questionnaire = createQuestionnaire(QuestionnaireType.QUIZ)
    val content = createChoiceContent(questionnaire.id!!, null)
    val round = startLiveContent(content.id!!)
    publishingService.stopLiveContent(questionnaire.id!!)
    val reopened = startLiveContent(content.id!!)
    Assertions.assertEquals(round.id, reopened.id)
    Assertions.assertFalse(reopened.closed)
    Assertions.assertNull(reopened.answeringEndTime)
    Assertions.assertTrue(reopened.acceptsAnswers())
  }

  @Test
  fun shouldNotExtendRemainderWhenStoppedTwiceBeforeReopening() {
    val questionnaire = createQuestionnaire(QuestionnaireType.QUIZ)
    val content = createChoiceContent(questionnaire.id!!, DURATION)
    val round = startLiveContent(content.id!!)
    publishingService.stopLiveContent(questionnaire.id!!)
    val afterFirstStop = reload(round)
    val pausedAtFirstStop = afterFirstStop.answeringPausedAt!!
    // A double stop must not let the round "gain" time by re-stamping the pause instant later.
    publishingService.stopLiveContent(questionnaire.id!!)
    val afterSecondStop = reload(round)
    Assertions.assertEquals(pausedAtFirstStop, afterSecondStop.answeringPausedAt)
    Assertions.assertEquals(afterFirstStop.answeringEndTime, afterSecondStop.answeringEndTime)
  }

  @Test
  fun shouldResumeCorrectlyWhenReturningToPreviouslyLiveContent() {
    val questionnaire = createQuestionnaire(QuestionnaireType.QUIZ)
    val contentA = createChoiceContent(questionnaire.id!!, DURATION)
    val contentB = createChoiceContent(questionnaire.id!!, DURATION)
    val roundA = startLiveContent(contentA.id!!)
    Thread.sleep(500) // let a chunk of A's countdown elapse before navigating away
    val beforeSwitchToB = Instant.now()
    // Starting B closes A's round via the previously-active-round path, not stopLiveContent.
    publishingService.startLiveContent(contentB.id!!)
    val stoppedRoundA = reload(roundA)
    Assertions.assertTrue(stoppedRoundA.closed)
    Assertions.assertNotNull(stoppedRoundA.answeringPausedAt)
    Assertions.assertFalse(stoppedRoundA.answeringPausedAt!!.isBefore(beforeSwitchToB))

    val beforeReopenA = Instant.now()
    val reopenedRoundA = startLiveContent(contentA.id!!)
    Assertions.assertEquals(roundA.id, reopenedRoundA.id)
    Assertions.assertFalse(reopenedRoundA.closed)
    Assertions.assertNotNull(reopenedRoundA.answeringEndTime)
    // Resumed with the leftover countdown, not a fresh full duration.
    Assertions.assertTrue(
        reopenedRoundA.answeringEndTime!!.isBefore(beforeReopenA.plusSeconds(DURATION.toLong())))
    Assertions.assertTrue(reopenedRoundA.acceptsAnswers())
  }

  @Test
  fun shouldRecordAnsweringDurationOnAnswer() {
    val questionnaire = createQuestionnaire(QuestionnaireType.QUIZ)
    val content = createChoiceContent(questionnaire.id!!, DURATION)
    val round = startLiveContent(content.id!!)
    Thread.sleep(50)
    val answer = answerService.createAnswer(round.id!!, ChoiceAnswerFormatData(listOf(0)))
    Assertions.assertNotNull(answer.durationMs)
    Assertions.assertTrue(answer.durationMs!! > 0)
    Assertions.assertTrue(answer.durationMs!! < DURATION * 1000)
  }

  @Test
  fun shouldNotRecordAnsweringDurationForUntimedContent() {
    val questionnaire = createQuestionnaire(QuestionnaireType.QUIZ)
    val content = createChoiceContent(questionnaire.id!!, null)
    val round = startLiveContent(content.id!!)
    val answer = answerService.createAnswer(round.id!!, ChoiceAnswerFormatData(listOf(0)))
    Assertions.assertNull(answer.durationMs)
  }

  @Test
  fun shouldKeepAnsweringDurationUnchangedAfterReopeningRound() {
    val questionnaire = createQuestionnaire(QuestionnaireType.QUIZ)
    val content = createChoiceContent(questionnaire.id!!, DURATION)
    val round = startLiveContent(content.id!!)
    Thread.sleep(50)
    val answer = answerService.createAnswer(round.id!!, ChoiceAnswerFormatData(listOf(0)))
    Assertions.assertNotNull(answer.durationMs)
    Assertions.assertTrue(answer.durationMs!! > 0)
    publishingService.stopLiveContent(questionnaire.id!!)
    val reopenedRound = startLiveContent(content.id!!)
    Assertions.assertEquals(round.id, reopenedRound.id)
    val reloadedAnswer = answerService.findByRoundIdAndAuditMetadataCreatedBy(round.id!!, USER_ID)!!
    Assertions.assertEquals(answer.durationMs, reloadedAnswer.durationMs)
  }

  @Test
  fun shouldExposeAnswerableAcrossStateDiagram() {
    val questionnaire = createQuestionnaire(QuestionnaireType.QUIZ)

    val untimedContent = createChoiceContent(questionnaire.id!!, null)
    Assertions.assertFalse(roundQueryController.answerable(untimedContent.activeRound!!))
    val runningUntimed = startLiveContent(untimedContent.id!!)
    Assertions.assertTrue(roundQueryController.answerable(runningUntimed))
    publishingService.stopLiveContent(questionnaire.id!!)
    Assertions.assertFalse(roundQueryController.answerable(reload(runningUntimed)))

    val timedContent = createChoiceContent(questionnaire.id!!, DURATION)
    Assertions.assertFalse(roundQueryController.answerable(timedContent.activeRound!!))
    val runningTimed = startLiveContent(timedContent.id!!)
    Assertions.assertTrue(roundQueryController.answerable(runningTimed))
    val expiredNotStopped = expire(reload(runningTimed))
    Assertions.assertFalse(roundQueryController.answerable(expiredNotStopped))
    publishingService.stopLiveContent(questionnaire.id!!)
    Assertions.assertFalse(roundQueryController.answerable(reload(runningTimed)))

    val timedContent2 = createChoiceContent(questionnaire.id!!, DURATION)
    val runningTimed2 = startLiveContent(timedContent2.id!!)
    publishingService.stopLiveContent(questionnaire.id!!)
    Assertions.assertFalse(roundQueryController.answerable(reload(runningTimed2)))
  }
}
