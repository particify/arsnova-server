/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.questionnaire

import java.util.UUID
import net.particify.arsnova.core4.questionnaire.template.TemplatePermissionEvaluator
import net.particify.arsnova.core4.questionnaire.template.TemplateRepository
import net.particify.arsnova.core4.user.User
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.Mockito

private val ROOM_ID = UUID.randomUUID()

class QuestionnairePermissionEvaluatorTest {
  private val evaluator =
      QuestionnairePermissionEvaluator(
          Mockito.mock(QuestionnaireRepository::class.java),
          TemplatePermissionEvaluator(Mockito.mock(TemplateRepository::class.java)))

  /** The room permission the check is delegated to, which decides participant versus moderator. */
  private fun delegatedRoomPermission(published: Boolean, leaderboardEnabled: Boolean?): Any? {
    val questionnaire =
        Questionnaire(
            id = UUID.randomUUID(),
            roomId = ROOM_ID,
            published = published,
            leaderboardEnabled = leaderboardEnabled)
    val reference =
        evaluator.hasPermission(User(), questionnaire, "readLeaderboard").permissionReference
    Assertions.assertEquals("Room", reference?.targetClassName)
    Assertions.assertEquals(ROOM_ID, reference?.id)
    return reference?.permission
  }

  @Test
  fun shouldLetParticipantsReadEnabledLeaderboardOfPublishedQuestionnaire() {
    Assertions.assertEquals("read", delegatedRoomPermission(true, true))
  }

  @Test
  fun shouldRequireModeratorForDisabledLeaderboard() {
    Assertions.assertEquals("moderate", delegatedRoomPermission(true, false))
    Assertions.assertEquals("moderate", delegatedRoomPermission(true, null))
  }

  @Test
  fun shouldRequireModeratorForLeaderboardOfUnpublishedQuestionnaire() {
    Assertions.assertEquals("moderate", delegatedRoomPermission(false, true))
  }
}
