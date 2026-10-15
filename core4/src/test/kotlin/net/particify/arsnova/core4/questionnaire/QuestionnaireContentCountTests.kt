/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire

import java.util.UUID
import net.particify.arsnova.core4.TestcontainersConfiguration
import net.particify.arsnova.core4.questionnaire.content.ChoiceContentFormatData
import net.particify.arsnova.core4.questionnaire.content.Content
import net.particify.arsnova.core4.questionnaire.content.ContentServiceImpl
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnairePublishingMode
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnaireServiceImpl
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnaireType
import net.particify.arsnova.core4.questionnaire.questionnaire.api.QuestionnaireQueryController
import net.particify.arsnova.core4.room.Membership
import net.particify.arsnova.core4.room.Room
import net.particify.arsnova.core4.room.RoomRole
import net.particify.arsnova.core4.room.internal.MembershipServiceImpl
import net.particify.arsnova.core4.user.User
import net.particify.arsnova.core4.user.UserService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.ActiveProfiles

/** Account from the `dev` Liquibase fixtures which owns [ROOM_ID]. */
private val OWNER_ID = UUID.fromString("43389844-708b-469d-8d22-abdd2eb88777")
private val ROOM_ID = UUID.fromString("659f7444-d5e3-41bc-b1c7-c1e3b085cd56")

/** Account creation publishes an event, which counting contents does not depend on. */
private const val EXTERNALIZATION_PROPERTY = "spring.modulith.events.externalization.enabled=false"

/**
 * Participants see the steps of contents which are not released yet, locked, so the count covers
 * every content of the questionnaire rather than only the released ones.
 */
@SpringBootTest(properties = [EXTERNALIZATION_PROPERTY])
@ActiveProfiles("test")
@Import(TestcontainersConfiguration::class)
class QuestionnaireContentCountTests {
  @Autowired lateinit var contentService: ContentServiceImpl
  @Autowired lateinit var membershipService: MembershipServiceImpl
  @Autowired lateinit var questionnaireQueryController: QuestionnaireQueryController
  @Autowired lateinit var questionnaireService: QuestionnaireServiceImpl
  @Autowired lateinit var userService: UserService

  @AfterEach
  fun clearAuthentication() {
    SecurityContextHolder.clearContext()
  }

  private fun authenticate(user: User) {
    SecurityContextHolder.getContext().authentication =
        UsernamePasswordAuthenticationToken(user, null, user.authorities)
  }

  private fun createContent(questionnaireId: UUID, body: String): Content =
      contentService.create(
          questionnaireId,
          body,
          ChoiceContentFormatData(listOf("A", "B"), null, false),
          null,
          true,
          null)

  @Test
  fun shouldCountUnreleasedContentsForParticipants() {
    authenticate(userService.loadUserById(OWNER_ID)!!)
    val created =
        questionnaireService.create(ROOM_ID, "Count ${UUID.randomUUID()}", QuestionnaireType.SURVEY)
    questionnaireService.update(
        created.id!!, null, true, QuestionnairePublishingMode.UP_TO, null, null, null)
    val first = createContent(created.id!!, "First")
    createContent(created.id!!, "Second")

    val participant = userService.createAccount()
    membershipService.save(
        Membership(
            room = Room(id = ROOM_ID),
            user = User(id = participant.id),
            role = RoomRole.PARTICIPANT))
    authenticate(participant)
    val questionnaire = questionnaireService.findByIdOrNull(created.id!!)!!

    Assertions.assertEquals(first.id, questionnaire.publishedUpToContent?.id)
    Assertions.assertEquals(2, questionnaireQueryController.contentCount(questionnaire))
  }
}
