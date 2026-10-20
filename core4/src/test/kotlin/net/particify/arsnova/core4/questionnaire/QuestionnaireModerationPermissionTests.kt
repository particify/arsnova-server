/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire

import java.util.Locale
import java.util.UUID
import net.particify.arsnova.core4.TestcontainersConfiguration
import net.particify.arsnova.core4.questionnaire.answer.api.LeaderboardQueryController
import net.particify.arsnova.core4.questionnaire.content.ChoiceContentFormatData
import net.particify.arsnova.core4.questionnaire.content.Content
import net.particify.arsnova.core4.questionnaire.content.ContentServiceImpl
import net.particify.arsnova.core4.questionnaire.content.api.ContentQueryController
import net.particify.arsnova.core4.questionnaire.questionnaire.Questionnaire
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnaireServiceImpl
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnaireType
import net.particify.arsnova.core4.questionnaire.questionnaire.api.QuestionnaireQueryController
import net.particify.arsnova.core4.questionnaire.round.api.RoundQueryController
import net.particify.arsnova.core4.room.Membership
import net.particify.arsnova.core4.room.Room
import net.particify.arsnova.core4.room.RoomRole
import net.particify.arsnova.core4.room.internal.MembershipServiceImpl
import net.particify.arsnova.core4.user.User
import net.particify.arsnova.core4.user.UserService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.ActiveProfiles

/** Account from the `dev` Liquibase fixtures which owns [ROOM_ID]. */
private val OWNER_ID = UUID.fromString("43389844-708b-469d-8d22-abdd2eb88777")
private val ROOM_ID = UUID.fromString("659f7444-d5e3-41bc-b1c7-c1e3b085cd56")

/** Account creation publishes an event, which permission evaluation does not depend on. */
private const val EXTERNALIZATION_PROPERTY = "spring.modulith.events.externalization.enabled=false"

/**
 * Moderators see a content's answer counts and hidden answers before the statistics are published,
 * so the reads leading there have to let them through as well.
 */
@SpringBootTest(properties = [EXTERNALIZATION_PROPERTY])
@ActiveProfiles("test")
@Import(TestcontainersConfiguration::class)
class QuestionnaireModerationPermissionTests {
  @Autowired lateinit var contentQueryController: ContentQueryController
  @Autowired lateinit var contentService: ContentServiceImpl
  @Autowired lateinit var leaderboardQueryController: LeaderboardQueryController
  @Autowired lateinit var membershipService: MembershipServiceImpl
  @Autowired lateinit var questionnaireQueryController: QuestionnaireQueryController
  @Autowired lateinit var questionnaireService: QuestionnaireServiceImpl
  @Autowired lateinit var roundQueryController: RoundQueryController
  @Autowired lateinit var userService: UserService

  private lateinit var questionnaire: Questionnaire
  private lateinit var content: Content

  /** Published, but with its statistics withheld from participants. */
  @BeforeEach
  fun createQuestionnaire() {
    authenticate(userService.loadUserById(OWNER_ID)!!)
    val created =
        questionnaireService.create(
            ROOM_ID, "Moderation ${UUID.randomUUID()}", QuestionnaireType.SURVEY)
    questionnaireService.updatePublished(created.id!!, true)
    questionnaire = questionnaireService.update(created.id!!, null, null, null, false, null, null)
    content =
        contentService.create(
            questionnaire.id!!,
            "Choice",
            ChoiceContentFormatData(listOf("A", "B"), null, false),
            null,
            true,
            null)
  }

  @AfterEach
  fun clearAuthentication() {
    SecurityContextHolder.clearContext()
  }

  private fun authenticate(user: User) {
    SecurityContextHolder.getContext().authentication =
        UsernamePasswordAuthenticationToken(user, null, user.authorities)
  }

  /** Created after the questionnaire, so that auditing saw the owner as its creator. */
  private fun authenticateMember(role: RoomRole): User {
    val user = userService.createAccount()
    membershipService.save(
        Membership(room = Room(id = ROOM_ID), user = User(id = user.id), role = role))
    authenticate(user)
    return user
  }

  private fun roundId(): UUID = content.activeRound!!.id!!

  /** Run while the owner is still authenticated, so before [authenticateMember]. */
  private fun unpublishQuestionnaire() {
    questionnaireService.updatePublished(questionnaire.id!!, false)
  }

  @Test
  fun shouldLetModeratorListQuestionnaires() {
    authenticateMember(RoomRole.MODERATOR)
    val ids = questionnaireQueryController.questionnairesByRoomId(ROOM_ID).map { it.id }
    Assertions.assertTrue(questionnaire.id in ids)
  }

  @Test
  fun shouldNotLetParticipantListQuestionnaires() {
    authenticateMember(RoomRole.PARTICIPANT)
    Assertions.assertThrows(AccessDeniedException::class.java) {
      questionnaireQueryController.questionnairesByRoomId(ROOM_ID)
    }
  }

  @Test
  fun shouldLetModeratorSeeUnpublishedStatistics() {
    authenticateMember(RoomRole.MODERATOR)
    Assertions.assertNotNull(roundQueryController.questionnaireChoiceRoundStatsByRoundId(roundId()))
  }

  @Test
  fun shouldNotLetParticipantSeeUnpublishedStatistics() {
    authenticateMember(RoomRole.PARTICIPANT)
    Assertions.assertThrows(AccessDeniedException::class.java) {
      roundQueryController.questionnaireChoiceRoundStatsByRoundId(roundId())
    }
  }

  @Test
  fun shouldLetModeratorOpenUnpublishedQuestionnaireAndItsContents() {
    unpublishQuestionnaire()
    authenticateMember(RoomRole.MODERATOR)
    Assertions.assertEquals(
        questionnaire.id, questionnaireQueryController.questionnaireById(questionnaire.id!!).id)
    val contents = contentQueryController.questionnaireContentsByQuestionnaireId(questionnaire.id!!)
    Assertions.assertEquals(listOf(content.id), contents.content.map { it.id })
    Assertions.assertEquals(
        content.id, contentQueryController.questionnaireContentById(content.id!!).id)
  }

  @Test
  fun shouldNotLetParticipantOpenUnpublishedQuestionnaireOrItsContents() {
    unpublishQuestionnaire()
    authenticateMember(RoomRole.PARTICIPANT)
    Assertions.assertThrows(AccessDeniedException::class.java) {
      questionnaireQueryController.questionnaireById(questionnaire.id!!)
    }
    Assertions.assertThrows(AccessDeniedException::class.java) {
      contentQueryController.questionnaireContentsByQuestionnaireId(questionnaire.id!!)
    }
    Assertions.assertThrows(AccessDeniedException::class.java) {
      contentQueryController.questionnaireContentById(content.id!!)
    }
  }

  /** The fixture's questionnaire does not enable the leaderboard for participants. */
  @Test
  fun shouldLetModeratorSeeTheLeaderboardParticipantsCannot() {
    val moderator = authenticateMember(RoomRole.MODERATOR)
    Assertions.assertNotNull(
        leaderboardQueryController.questionnaireLeaderboardByQuestionnaireId(
            questionnaire.id!!, null, null, moderator, Locale.ENGLISH))
  }

  @Test
  fun shouldNotLetParticipantSeeADisabledLeaderboard() {
    val participant = authenticateMember(RoomRole.PARTICIPANT)
    Assertions.assertThrows(AccessDeniedException::class.java) {
      leaderboardQueryController.questionnaireLeaderboardByQuestionnaireId(
          questionnaire.id!!, null, null, participant, Locale.ENGLISH)
    }
  }
}
