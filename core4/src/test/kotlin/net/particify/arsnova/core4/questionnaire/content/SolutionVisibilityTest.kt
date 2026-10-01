/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.content

import java.util.UUID
import net.particify.arsnova.core4.questionnaire.questionnaire.Questionnaire
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.security.access.PermissionEvaluator
import org.springframework.security.authentication.TestingAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder

private val CONTENT_ID = UUID.randomUUID()
private val ROOM_ID = UUID.randomUUID()

class SolutionVisibilityTest {
  private val permissionEvaluator = Mockito.mock(PermissionEvaluator::class.java)
  private val solutionVisibility = SolutionVisibility(permissionEvaluator)
  private val authentication = TestingAuthenticationToken("user", null, "ROLE_USER")

  @AfterEach
  fun clearAuthentication() {
    SecurityContextHolder.clearContext()
  }

  private fun content(correctOptionsPublished: Boolean?, roomId: UUID? = ROOM_ID) =
      Content(
          id = CONTENT_ID,
          questionnaire =
              Questionnaire(
                  id = UUID.randomUUID(),
                  roomId = roomId,
                  correctOptionsPublished = correctOptionsPublished))

  private fun givenPermissions(mayWriteContent: Boolean, mayModerateRoom: Boolean) {
    SecurityContextHolder.getContext().authentication = authentication
    Mockito.`when`(
            permissionEvaluator.hasPermission(authentication, CONTENT_ID, "Content", "write"))
        .thenReturn(mayWriteContent)
    Mockito.`when`(permissionEvaluator.hasPermission(authentication, ROOM_ID, "Room", "moderate"))
        .thenReturn(mayModerateRoom)
  }

  @Test
  fun shouldShowPublishedSolutionToEveryoneWithoutCheckingPermissions() {
    Assertions.assertTrue(solutionVisibility.maySeeSolution(content(true)))
    Mockito.verifyNoInteractions(permissionEvaluator)
  }

  @Test
  fun shouldShowUnpublishedSolutionToEditors() {
    givenPermissions(mayWriteContent = true, mayModerateRoom = true)
    Assertions.assertTrue(solutionVisibility.maySeeSolution(content(false)))
  }

  @Test
  fun shouldShowUnpublishedSolutionToModerators() {
    givenPermissions(mayWriteContent = false, mayModerateRoom = true)
    Assertions.assertTrue(solutionVisibility.maySeeSolution(content(null)))
  }

  @Test
  fun shouldWithholdUnpublishedSolutionFromParticipants() {
    givenPermissions(mayWriteContent = false, mayModerateRoom = false)
    Assertions.assertFalse(solutionVisibility.maySeeSolution(content(false)))
    Assertions.assertFalse(solutionVisibility.maySeeSolution(content(null)))
  }

  @Test
  fun shouldWithholdUnpublishedSolutionWithoutAuthentication() {
    Assertions.assertFalse(solutionVisibility.maySeeSolution(content(false)))
    Mockito.verifyNoInteractions(permissionEvaluator)
  }

  @Test
  fun shouldOnlyConsultContentPermissionWithoutRoom() {
    givenPermissions(mayWriteContent = false, mayModerateRoom = true)
    Assertions.assertFalse(solutionVisibility.maySeeSolution(content(false, roomId = null)))
  }
}
