/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire

import java.util.Optional
import java.util.UUID
import net.particify.arsnova.core4.common.AuditMetadata
import net.particify.arsnova.core4.questionnaire.answer.AnswerPermissionEvaluator
import net.particify.arsnova.core4.questionnaire.answer.AnswerRepository
import net.particify.arsnova.core4.questionnaire.content.Content
import net.particify.arsnova.core4.questionnaire.content.ContentPermissionEvaluator
import net.particify.arsnova.core4.questionnaire.content.ContentRepository
import net.particify.arsnova.core4.questionnaire.questionnaire.Questionnaire
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnairePermissionEvaluator
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnairePublishingMode
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnaireRepository
import net.particify.arsnova.core4.questionnaire.round.Round
import net.particify.arsnova.core4.questionnaire.round.RoundPermissionEvaluator
import net.particify.arsnova.core4.questionnaire.round.RoundRepository
import net.particify.arsnova.core4.questionnaire.template.Template
import net.particify.arsnova.core4.questionnaire.template.TemplatePermissionEvaluator
import net.particify.arsnova.core4.questionnaire.template.TemplateRepository
import net.particify.arsnova.core4.security.internal.DelegatingPermissionEvaluator
import net.particify.arsnova.core4.user.User
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.security.authentication.TestingAuthenticationToken

private val CREATOR_ID = UUID.randomUUID()

/**
 * A room-less questionnaire holds a template's contents and shares the template's id, so the
 * template decides who may read and change it, and nothing about presenting it is accessible.
 */
class RoomlessPermissionTest {
  private val templateRepository = Mockito.mock(TemplateRepository::class.java)
  private val questionnaireRepository = Mockito.mock(QuestionnaireRepository::class.java)
  private val contentRepository = Mockito.mock(ContentRepository::class.java)
  private val templatePermissionEvaluator = TemplatePermissionEvaluator(templateRepository)
  private val permissionEvaluator =
      DelegatingPermissionEvaluator(
          listOf(
              AnswerPermissionEvaluator(Mockito.mock(AnswerRepository::class.java)),
              ContentPermissionEvaluator(contentRepository),
              QuestionnairePermissionEvaluator(
                  questionnaireRepository, templatePermissionEvaluator),
              RoundPermissionEvaluator(Mockito.mock(RoundRepository::class.java)),
              templatePermissionEvaluator))

  // Unpublished and not releasing anything, which would hide a room questionnaire's contents.
  private val roomless =
      Questionnaire(
          id = UUID.randomUUID(),
          published = false,
          publishingMode = QuestionnairePublishingMode.LIVE)
  private val content = Content(id = UUID.randomUUID(), questionnaire = roomless, position = 3)

  private fun givenTemplate(published: Boolean) {
    val template =
        Template(
            id = roomless.id,
            published = published,
            auditMetadata = AuditMetadata(createdBy = CREATOR_ID))
    Mockito.`when`(templateRepository.findById(roomless.id!!)).thenReturn(Optional.of(template))
    Mockito.`when`(questionnaireRepository.findById(roomless.id!!))
        .thenReturn(Optional.of(roomless))
  }

  private fun allowed(userId: UUID, target: Any, permission: String) =
      permissionEvaluator.hasPermission(
          TestingAuthenticationToken(User(id = userId), null), target, permission)

  @Test
  fun shouldLetTheCreatorReadAndChangeButNotDeleteOrRankTheQuestionnaire() {
    givenTemplate(published = false)
    Assertions.assertEquals(
        listOf(true, true, false, false),
        listOf("read", "write", "delete", "readLeaderboard").map {
          allowed(CREATOR_ID, roomless, it)
        })
  }

  @Test
  fun shouldLetOthersOnlyReadAPublishedTemplate() {
    givenTemplate(published = true)
    Assertions.assertTrue(allowed(UUID.randomUUID(), roomless, "read"))
    Assertions.assertFalse(allowed(UUID.randomUUID(), roomless, "write"))
    givenTemplate(published = false)
    Assertions.assertFalse(allowed(UUID.randomUUID(), roomless, "read"))
  }

  @Test
  fun shouldShowEveryContentToTemplateReadersRegardlessOfPublishing() {
    givenTemplate(published = true)
    Assertions.assertTrue(allowed(UUID.randomUUID(), content, "read"))
    Assertions.assertFalse(allowed(UUID.randomUUID(), content, "write"))
    Assertions.assertTrue(allowed(CREATOR_ID, content, "write"))
  }

  @Test
  fun shouldDenyEverythingAboutRoundsOfATemplate() {
    givenTemplate(published = true)
    val round = Round(id = UUID.randomUUID(), content = content)
    listOf("read", "write", "readStatistics", "delete").forEach {
      Assertions.assertFalse(allowed(CREATOR_ID, round, it), it)
    }
  }
}
