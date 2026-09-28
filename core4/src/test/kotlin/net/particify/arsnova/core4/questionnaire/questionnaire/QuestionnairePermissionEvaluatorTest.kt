/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.questionnaire

import java.util.UUID
import net.particify.arsnova.core4.user.User
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.Mockito

private val ROOM_ID = UUID.randomUUID()

class QuestionnairePermissionEvaluatorTest {
  private val evaluator =
      QuestionnairePermissionEvaluator(Mockito.mock(QuestionnaireRepository::class.java))

  /** The room permission the check is delegated to, which decides participant versus editor. */
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
  fun shouldRequireEditorForDisabledLeaderboard() {
    Assertions.assertEquals("write", delegatedRoomPermission(true, false))
    Assertions.assertEquals("write", delegatedRoomPermission(true, null))
  }

  @Test
  fun shouldRequireEditorForLeaderboardOfUnpublishedQuestionnaire() {
    Assertions.assertEquals("write", delegatedRoomPermission(false, true))
  }
}
