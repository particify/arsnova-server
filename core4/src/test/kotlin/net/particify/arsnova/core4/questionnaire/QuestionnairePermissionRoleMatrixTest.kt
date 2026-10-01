/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire

import java.util.Optional
import java.util.UUID
import net.particify.arsnova.core4.questionnaire.answer.Answer
import net.particify.arsnova.core4.questionnaire.answer.AnswerPermissionEvaluator
import net.particify.arsnova.core4.questionnaire.answer.AnswerRepository
import net.particify.arsnova.core4.questionnaire.content.Content
import net.particify.arsnova.core4.questionnaire.content.ContentPermissionEvaluator
import net.particify.arsnova.core4.questionnaire.content.ContentRepository
import net.particify.arsnova.core4.questionnaire.exception.AnswerNotFoundException
import net.particify.arsnova.core4.questionnaire.exception.ContentNotFoundException
import net.particify.arsnova.core4.questionnaire.exception.QuestionnaireNotFoundException
import net.particify.arsnova.core4.questionnaire.exception.RoundNotFoundException
import net.particify.arsnova.core4.questionnaire.questionnaire.Questionnaire
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnairePermissionEvaluator
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnairePublishingMode
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnaireRepository
import net.particify.arsnova.core4.questionnaire.round.Round
import net.particify.arsnova.core4.questionnaire.round.RoundPermissionEvaluator
import net.particify.arsnova.core4.questionnaire.round.RoundRepository
import net.particify.arsnova.core4.room.Membership
import net.particify.arsnova.core4.room.Room
import net.particify.arsnova.core4.room.RoomPermissionEvaluator
import net.particify.arsnova.core4.room.RoomRole
import net.particify.arsnova.core4.room.internal.MembershipRepository
import net.particify.arsnova.core4.room.internal.RoomRepository
import net.particify.arsnova.core4.security.internal.DelegatingPermissionEvaluator
import net.particify.arsnova.core4.user.User
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.security.authentication.TestingAuthenticationToken

private val ROOM_ID = UUID.randomUUID()

private const val DELETE = "delete"
private const val READ = "read"
private const val READ_STATISTICS = "readStatistics"
private const val WRITE = "write"

/** Every role, plus a user without membership in the room. */
private val ROLES =
    listOf(RoomRole.OWNER, RoomRole.EDITOR, RoomRole.MODERATOR, RoomRole.PARTICIPANT, null)
private val EVERY_MEMBER =
    setOf(RoomRole.OWNER, RoomRole.EDITOR, RoomRole.MODERATOR, RoomRole.PARTICIPANT)
private val EDITORS = setOf(RoomRole.OWNER, RoomRole.EDITOR)

/**
 * Resolves each questionnaire evaluator's delegation chain up to the room, where the role decides.
 * The questionnaire evaluators only pick which room permission applies, so the role matrix is only
 * observable through the whole chain.
 */
class QuestionnairePermissionRoleMatrixTest {
  private val membershipRepository = Mockito.mock(MembershipRepository::class.java)
  private val roomRepository = Mockito.mock(RoomRepository::class.java)
  private val questionnaireRepository = Mockito.mock(QuestionnaireRepository::class.java)
  private val contentRepository = Mockito.mock(ContentRepository::class.java)
  private val roundRepository = Mockito.mock(RoundRepository::class.java)
  private val answerRepository = Mockito.mock(AnswerRepository::class.java)
  private val answerEvaluator = AnswerPermissionEvaluator(answerRepository)
  private val contentEvaluator = ContentPermissionEvaluator(contentRepository)
  private val questionnaireEvaluator = QuestionnairePermissionEvaluator(questionnaireRepository)
  private val roundEvaluator = RoundPermissionEvaluator(roundRepository)
  private val permissionEvaluator =
      DelegatingPermissionEvaluator(
          listOf(
              answerEvaluator,
              contentEvaluator,
              questionnaireEvaluator,
              RoomPermissionEvaluator(membershipRepository, roomRepository),
              roundEvaluator))

  init {
    Mockito.`when`(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(Room(id = ROOM_ID)))
  }

  private fun questionnaire(
      published: Boolean = true,
      publishingMode: QuestionnairePublishingMode? = QuestionnairePublishingMode.ALL,
      statisticsPublished: Boolean? = null,
  ): Questionnaire {
    val questionnaire =
        Questionnaire(
            id = UUID.randomUUID(),
            roomId = ROOM_ID,
            published = published,
            publishingMode = publishingMode,
            statisticsPublished = statisticsPublished)
    Mockito.`when`(questionnaireRepository.findById(questionnaire.id!!))
        .thenReturn(Optional.of(questionnaire))
    return questionnaire
  }

  private fun content(questionnaire: Questionnaire = questionnaire(), position: Int = 0): Content {
    val content =
        Content(id = UUID.randomUUID(), questionnaire = questionnaire, position = position)
    Mockito.`when`(contentRepository.findById(content.id!!)).thenReturn(Optional.of(content))
    return content
  }

  private fun round(content: Content = content()): Round {
    val round = Round(id = UUID.randomUUID(), content = content)
    Mockito.`when`(roundRepository.findById(round.id!!)).thenReturn(Optional.of(round))
    return round
  }

  private fun answer(round: Round = round()) = Answer(id = UUID.randomUUID(), round = round)

  private fun userWithRole(role: RoomRole?): User {
    val user = User(id = UUID.randomUUID())
    val membership = role?.let { Membership(room = Room(id = ROOM_ID), user = user, role = it) }
    Mockito.`when`(membershipRepository.findOneByRoomIdAndUserId(ROOM_ID, user.id!!))
        .thenReturn(membership)
    return user
  }

  private fun assertGrantedTo(target: Any, permission: String, granted: Set<RoomRole>) {
    ROLES.forEach { role ->
      val authentication = TestingAuthenticationToken(userWithRole(role), null)
      Assertions.assertEquals(
          granted.contains(role),
          permissionEvaluator.hasPermission(authentication, target, permission),
          "$role $permission")
    }
  }

  @Nested
  inner class QuestionnaireEvaluator {
    @Test
    fun shouldRestrictWriteAndDeleteToEditors() {
      val questionnaire = questionnaire()
      assertGrantedTo(questionnaire, WRITE, EDITORS)
      assertGrantedTo(questionnaire, DELETE, EDITORS)
    }

    @Test
    fun shouldLetEveryMemberReadPublishedQuestionnaire() {
      assertGrantedTo(questionnaire(published = true), READ, EVERY_MEMBER)
    }

    @Test
    fun shouldRestrictReadOfUnpublishedQuestionnaireToEditors() {
      assertGrantedTo(questionnaire(published = false), READ, EDITORS)
    }

    @Test
    fun shouldDenyUnknownPermissionEvenToOwner() {
      assertGrantedTo(questionnaire(), READ_STATISTICS, setOf())
    }
  }

  @Nested
  inner class ContentEvaluator {
    @Test
    fun shouldRestrictWriteAndDeleteToEditors() {
      val content = content()
      assertGrantedTo(content, WRITE, EDITORS)
      assertGrantedTo(content, DELETE, EDITORS)
    }

    @Test
    fun shouldLetEveryMemberReadContentOfPublishedQuestionnaire() {
      assertGrantedTo(content(), READ, EVERY_MEMBER)
    }

    @Test
    fun shouldRestrictReadOfContentOfUnpublishedQuestionnaireToEditors() {
      assertGrantedTo(content(questionnaire(published = false)), READ, EDITORS)
    }

    @Test
    fun shouldLetEveryMemberReadContentUpToPublishedPosition() {
      listOf(QuestionnairePublishingMode.UP_TO, QuestionnairePublishingMode.LIVE).forEach { mode ->
        val questionnaire = questionnaire(publishingMode = mode)
        questionnaire.publishedUpToContent = content(questionnaire, position = 1)
        assertGrantedTo(content(questionnaire, position = 0), READ, EVERY_MEMBER)
        assertGrantedTo(content(questionnaire, position = 1), READ, EVERY_MEMBER)
        assertGrantedTo(content(questionnaire, position = 2), READ, EDITORS)
      }
    }

    @Test
    fun shouldRestrictReadToEditorsWhileNothingIsReleased() {
      listOf(QuestionnairePublishingMode.UP_TO, QuestionnairePublishingMode.LIVE, null).forEach {
        assertGrantedTo(content(questionnaire(publishingMode = it)), READ, EDITORS)
      }
    }

    @Test
    fun shouldDenyUnknownPermissionEvenToOwner() {
      assertGrantedTo(content(), READ_STATISTICS, setOf())
    }
  }

  @Nested
  inner class RoundEvaluator {
    @Test
    fun shouldRestrictWriteAndDeleteToEditors() {
      val round = round()
      assertGrantedTo(round, WRITE, EDITORS)
      assertGrantedTo(round, DELETE, EDITORS)
    }

    @Test
    fun shouldFollowContentVisibilityForRead() {
      assertGrantedTo(round(), READ, EVERY_MEMBER)
      assertGrantedTo(round(content(questionnaire(published = false))), READ, EDITORS)
    }

    @Test
    fun shouldLetEveryMemberReadPublishedStatistics() {
      assertGrantedTo(
          round(content(questionnaire(statisticsPublished = true))), READ_STATISTICS, EVERY_MEMBER)
    }

    @Test
    fun shouldRestrictUnpublishedStatisticsToEditors() {
      listOf(false, null).forEach {
        assertGrantedTo(
            round(content(questionnaire(statisticsPublished = it))), READ_STATISTICS, EDITORS)
      }
    }

    @Test
    fun shouldRestrictPublishedStatisticsOfUnreleasedContentToEditors() {
      val questionnaire =
          questionnaire(
              publishingMode = QuestionnairePublishingMode.UP_TO, statisticsPublished = true)
      assertGrantedTo(round(content(questionnaire)), READ_STATISTICS, EDITORS)
    }

    @Test
    fun shouldDenyUnknownPermissionEvenToOwner() {
      assertGrantedTo(round(), "moderate", setOf())
    }
  }

  @Nested
  inner class AnswerEvaluator {
    @Test
    fun shouldRestrictWriteAndDeleteToEditors() {
      val answer = answer()
      assertGrantedTo(answer, WRITE, EDITORS)
      assertGrantedTo(answer, DELETE, EDITORS)
    }

    @Test
    fun shouldFollowContentVisibilityForRead() {
      assertGrantedTo(answer(), READ, EVERY_MEMBER)
      assertGrantedTo(answer(round(content(questionnaire(published = false)))), READ, EDITORS)
    }

    @Test
    fun shouldNotGrantStatisticsThroughAnswer() {
      assertGrantedTo(
          answer(round(content(questionnaire(statisticsPublished = true)))),
          READ_STATISTICS,
          setOf())
    }
  }

  @Test
  fun shouldResolveOnlyTheirOwnTypes() {
    listOf(
            Triple(answerEvaluator, Answer::class, "Answer"),
            Triple(contentEvaluator, Content::class, "Content"),
            Triple(questionnaireEvaluator, Questionnaire::class, "Questionnaire"),
            Triple(roundEvaluator, Round::class, "Round"))
        .forEach { (evaluator, clazz, className) ->
          Assertions.assertTrue(evaluator.supports(clazz), className)
          Assertions.assertTrue(evaluator.supports(className), className)
          Assertions.assertFalse(evaluator.supports(Room::class), className)
          Assertions.assertFalse(evaluator.supports("Room"), className)
        }
  }

  @Test
  fun shouldRejectMissingOrMistypedKeys() {
    val missingId = UUID.randomUUID()
    Assertions.assertThrows(AnswerNotFoundException::class.java) {
      answerEvaluator.findOneByKey(missingId)
    }
    Assertions.assertThrows(ContentNotFoundException::class.java) {
      contentEvaluator.findOneByKey(missingId)
    }
    Assertions.assertThrows(QuestionnaireNotFoundException::class.java) {
      questionnaireEvaluator.findOneByKey(missingId)
    }
    Assertions.assertThrows(RoundNotFoundException::class.java) {
      roundEvaluator.findOneByKey(missingId)
    }
    listOf(answerEvaluator, contentEvaluator, questionnaireEvaluator, roundEvaluator).forEach {
      Assertions.assertThrows(IllegalStateException::class.java) {
        it.findOneByKey(missingId.toString())
      }
    }
  }
}
