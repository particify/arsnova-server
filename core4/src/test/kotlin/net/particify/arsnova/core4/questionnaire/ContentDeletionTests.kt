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
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnairePublishingServiceImpl
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnaireServiceImpl
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnaireType
import net.particify.arsnova.core4.user.UserService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.ActiveProfiles

private val ROOM_ID = UUID.fromString("3f9cecb5-6dbf-45cc-b630-4af700d474b6")
private val USER_ID = UUID.fromString("9c778494-9b52-424d-8c7f-f4936629facb")

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration::class)
class ContentDeletionTests {
  @Autowired lateinit var contentService: ContentServiceImpl
  @Autowired lateinit var publishingService: QuestionnairePublishingServiceImpl
  @Autowired lateinit var questionnaireService: QuestionnaireServiceImpl
  @Autowired lateinit var userService: UserService

  @BeforeEach
  fun authenticateUser() {
    val user = userService.loadUserById(USER_ID)!!
    SecurityContextHolder.getContext().authentication =
        UsernamePasswordAuthenticationToken(user, null, user.authorities)
  }

  @AfterEach
  fun clearAuthentication() {
    SecurityContextHolder.clearContext()
  }

  /** Creates a questionnaire with three contents, published up to the one at [publishedIndex]. */
  private fun createContents(publishedIndex: Int): List<Content> {
    val questionnaire =
        questionnaireService.create(
            ROOM_ID, "Deletion ${UUID.randomUUID()}", QuestionnaireType.SURVEY)
    questionnaireService.update(
        questionnaire.id!!, null, null, QuestionnairePublishingMode.UP_TO, null, null, null)
    val contents =
        listOf("First", "Second", "Third").map {
          contentService.create(
              questionnaire.id!!,
              it,
              ChoiceContentFormatData(listOf("A", "B"), listOf(0), false),
              null,
              true,
              null)
        }
    publishingService.publishUpToContent(questionnaire.id!!, contents[publishedIndex].id!!)
    return contents
  }

  private fun publishedUpToContentIdOf(content: Content) =
      questionnaireService.findByIdOrNull(content.questionnaire!!.id!!)!!.publishedUpToContent?.id

  @Test
  fun shouldMovePublishedRangeToPrecedingContentWhenDeletingItsLastContent() {
    val contents = createContents(publishedIndex = 1)
    contentService.delete(contents[1].id!!)
    Assertions.assertEquals(contents[0].id, publishedUpToContentIdOf(contents[0]))
  }

  @Test
  fun shouldClearPublishedRangeWhenDeletingTheFirstContentItEndsAt() {
    val contents = createContents(publishedIndex = 0)
    contentService.delete(contents[0].id!!)
    Assertions.assertNull(publishedUpToContentIdOf(contents[1]))
  }

  @Test
  fun shouldReleaseTheActiveRoundWhenDeletingThePresentedContent() {
    val questionnaire =
        questionnaireService.create(ROOM_ID, "Live ${UUID.randomUUID()}", QuestionnaireType.QUIZ)
    val presented =
        contentService.create(
            questionnaire.id!!,
            "Presented",
            ChoiceContentFormatData(listOf("A", "B"), listOf(0), false),
            null,
            true,
            null)
    publishingService.startLiveContent(presented.id!!)
    contentService.delete(presented.id!!)
    val reloaded = questionnaireService.findByIdOrNull(questionnaire.id!!)!!
    Assertions.assertNull(reloaded.activeRound)
    Assertions.assertNull(reloaded.publishedUpToContent)
  }

  @Test
  fun shouldDeleteQuestionnaireWhilePresentingAContent() {
    val questionnaire =
        questionnaireService.create(ROOM_ID, "Live ${UUID.randomUUID()}", QuestionnaireType.QUIZ)
    val presented =
        contentService.create(
            questionnaire.id!!,
            "Presented",
            ChoiceContentFormatData(listOf("A", "B"), listOf(0), false),
            null,
            true,
            null)
    publishingService.startLiveContent(presented.id!!)
    questionnaireService.delete(questionnaire.id!!)
    Assertions.assertNull(questionnaireService.findByIdOrNull(questionnaire.id!!))
    Assertions.assertNull(contentService.findByIdOrNull(presented.id!!))
  }

  @Test
  fun shouldKeepPublishedRangeWhenDeletingAContentOutsideIt() {
    val contents = createContents(publishedIndex = 0)
    contentService.delete(contents[2].id!!)
    Assertions.assertEquals(contents[0].id, publishedUpToContentIdOf(contents[0]))
  }
}
