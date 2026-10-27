/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.template

import java.util.UUID
import net.particify.arsnova.core4.TestcontainersConfiguration
import net.particify.arsnova.core4.common.exception.InvalidInputException
import net.particify.arsnova.core4.questionnaire.content.ChoiceContentFormatData
import net.particify.arsnova.core4.questionnaire.content.Content
import net.particify.arsnova.core4.questionnaire.content.ContentRepository
import net.particify.arsnova.core4.questionnaire.content.ContentServiceImpl
import net.particify.arsnova.core4.questionnaire.content.api.ContentMutationController
import net.particify.arsnova.core4.questionnaire.executeAsCurrentUser
import net.particify.arsnova.core4.questionnaire.questionnaire.Questionnaire
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnaireServiceImpl
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnaireType
import net.particify.arsnova.core4.questionnaire.template.api.CreateQuestionnaireFromTemplateInput
import net.particify.arsnova.core4.questionnaire.template.api.CreateTemplateFromQuestionnaireInput
import net.particify.arsnova.core4.questionnaire.template.api.TemplateMutationController
import net.particify.arsnova.core4.questionnaire.template.api.TemplateQueryController
import net.particify.arsnova.core4.questionnaire.template.api.UpdateTemplateInput
import net.particify.arsnova.core4.room.Room
import net.particify.arsnova.core4.room.internal.RoomServiceImpl
import net.particify.arsnova.core4.user.User
import net.particify.arsnova.core4.user.UserService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.ScrollPosition
import org.springframework.graphql.ExecutionGraphQlService
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.support.TransactionTemplate

/** Accounts from the `dev` Liquibase fixtures. */
private val CREATOR_ID = UUID.fromString("9c778494-9b52-424d-8c7f-f4936629facb")
private val OTHER_USER_ID = UUID.fromString("43389844-708b-469d-8d22-abdd2eb88777")

/** Owned by [OTHER_USER_ID]. */
private val OTHER_USERS_ROOM_ID = UUID.fromString("659f7444-d5e3-41bc-b1c7-c1e3b085cd56")

/** Room events reach RabbitMQ, which none of this depends on. */
private const val EXTERNALIZATION_PROPERTY = "spring.modulith.events.externalization.enabled=false"

/** How credits and licences travel along with contents through templates and their copies. */
@SpringBootTest(properties = [EXTERNALIZATION_PROPERTY])
@ActiveProfiles("test")
@Import(TestcontainersConfiguration::class)
class TemplateLicensingTests {
  @Autowired lateinit var contentMutationController: ContentMutationController
  @Autowired lateinit var contentRepository: ContentRepository
  @Autowired lateinit var contentService: ContentServiceImpl
  @Autowired lateinit var graphQlService: ExecutionGraphQlService
  @Autowired lateinit var questionnaireService: QuestionnaireServiceImpl
  @Autowired lateinit var roomService: RoomServiceImpl
  @Autowired lateinit var templateMutationController: TemplateMutationController
  @Autowired lateinit var templateQueryController: TemplateQueryController
  @Autowired lateinit var templateRepository: TemplateRepository
  @Autowired lateinit var transactionTemplate: TransactionTemplate
  @Autowired lateinit var userService: UserService

  private lateinit var creator: User

  @BeforeEach
  fun authenticateCreator() {
    creator = authenticate(CREATOR_ID)
  }

  @AfterEach
  fun clearAuthentication() {
    SecurityContextHolder.clearContext()
  }

  private fun authenticate(userId: UUID): User = authenticate(userService.loadUserById(userId)!!)

  private fun authenticate(user: User): User {
    SecurityContextHolder.getContext().authentication =
        UsernamePasswordAuthenticationToken(user, null, user.authorities)
    return user
  }

  private fun createRoom(user: User): Room =
      roomService.create(Room(shortId = roomService.generateShortId(), name = "Templates"), user)

  private fun addQuestion(questionnaireId: UUID): Content =
      contentService.create(
          questionnaireId,
          "Question ${UUID.randomUUID()}",
          ChoiceContentFormatData(listOf("A", "B"), listOf(0), false),
          null,
          true,
          null)

  private fun createQuiz(roomId: UUID, contentCount: Int): Questionnaire {
    val quiz =
        questionnaireService.create(roomId, "Quiz ${UUID.randomUUID()}", QuestionnaireType.QUIZ)
    repeat(contentCount) { addQuestion(quiz.id!!) }
    return quiz
  }

  private fun createTemplate(
      questionnaire: Questionnaire,
      published: Boolean = true,
      licenseId: String? = "CC-BY-4.0",
      attribution: String = "Someone"
  ): Template =
      templateMutationController.createTemplateFromQuestionnaire(
          CreateTemplateFromQuestionnaireInput(
              attribution = attribution,
              description = null,
              language = "en",
              licenseId = licenseId,
              name = "Template",
              published = published,
              questionnaireId = questionnaire.id!!,
              tagNames = null))

  /** A published template of the creator with three contents. */
  private fun createUpstreamTemplate(): Template =
      createTemplate(createQuiz(createRoom(creator).id!!, 3))

  private fun contentsOf(questionnaireId: UUID): List<Content> =
      contentRepository
          .findByQuestionnaireIdOrderByPosition(questionnaireId, ScrollPosition.offset())
          .toList()

  private fun copyForOtherUser(template: Template): Questionnaire {
    authenticate(OTHER_USER_ID)
    return templateMutationController.createQuestionnaireFromTemplate(
        CreateQuestionnaireFromTemplateInput(
            "Copy ${UUID.randomUUID()}", OTHER_USERS_ROOM_ID, template.id!!))
  }

  /** [OTHER_USER_ID]'s template made from a copy of the creator's template. */
  private fun createDerivedTemplate(upstream: Template): Template {
    val copy = copyForOtherUser(upstream)
    addQuestion(copy.id!!)
    return createTemplate(copy, attribution = "Deriver")
  }

  private fun copyForCreator(template: Template): Questionnaire {
    authenticate(creator)
    return templateMutationController.createQuestionnaireFromTemplate(
        CreateQuestionnaireFromTemplateInput(
            "Copy ${UUID.randomUUID()}", createRoom(creator).id!!, template.id!!))
  }

  private fun upstreamOf(questionnaireId: UUID): List<List<Pair<UUID, String?>>> =
      templateQueryController.templateContentAttributionsByQuestionnaireId(questionnaireId).map {
          attribution ->
        attribution.upstreamAttributions.map { it.templateId to it.attribution }
      }

  @Test
  fun shouldCreditTheUpstreamTemplateInCopiesOfADerivedTemplate() {
    val upstream = createUpstreamTemplate()
    val derived = createDerivedTemplate(upstream)
    val copy = copyForCreator(derived)

    val attributions =
        templateQueryController.templateContentAttributionsByQuestionnaireId(copy.id!!)
    Assertions.assertEquals(4, attributions.size)
    attributions.forEach {
      Assertions.assertEquals(derived.id, it.templateId)
      Assertions.assertEquals("Deriver", it.attribution)
      Assertions.assertEquals("CC-BY-4.0", it.license?.id)
    }
    val credit = upstream.id!! to "Someone"
    Assertions.assertEquals(
        listOf(listOf(credit), listOf(credit), listOf(credit), listOf()), upstreamOf(copy.id!!))
    Assertions.assertEquals(
        "CC-BY-4.0", attributions.first().upstreamAttributions.single().license?.id)

    templateMutationController.deleteTemplate(upstream.id!!)
    Assertions.assertEquals(
        listOf(listOf(credit), listOf(credit), listOf(credit), listOf()), upstreamOf(copy.id!!))
  }

  @Test
  fun shouldServeTheUpstreamCreditsThroughTheSchema() {
    val upstream = createUpstreamTemplate()
    val copy = copyForCreator(createDerivedTemplate(upstream))
    val response =
        graphQlService.executeAsCurrentUser(
            """
            {
              templateContentAttributionsByQuestionnaireId(questionnaireId: "${copy.id}") {
                upstreamAttributions {
                  attribution
                  license {
                    id
                  }
                  templateId
                }
              }
            }
            """
                .trimIndent())
    Assertions.assertTrue(response.errors.isEmpty(), response.errors.toString())
    val attributions =
        response
            .getData<Map<String, List<Map<String, List<Map<String, Any?>>>>>>()!!
            .getValue("templateContentAttributionsByQuestionnaireId")
    Assertions.assertEquals(
        mapOf(
            "attribution" to "Someone",
            "license" to mapOf("id" to "CC-BY-4.0"),
            "templateId" to upstream.id.toString()),
        attributions.first().getValue("upstreamAttributions").single())
    Assertions.assertEquals(listOf<Any>(), attributions.last().getValue("upstreamAttributions"))
  }

  @Test
  fun shouldKeepTheUpstreamCreditsOfContentsMovedOutOfACopy() {
    val upstream = createUpstreamTemplate()
    val copy = copyForCreator(createDerivedTemplate(upstream))
    val own = questionnaireService.create(copy.roomId!!, "Own", QuestionnaireType.QUIZ)
    transactionTemplate.execute {
      contentMutationController.moveQuestionnaireContentToQuestionnaire(
          contentsOf(copy.id!!).first().id!!, own.id!!)
    }
    Assertions.assertEquals(listOf(listOf(upstream.id!! to "Someone")), upstreamOf(own.id!!))
  }

  @Test
  fun shouldRefreshTheUpstreamCreditsFromTheSource() {
    val upstream = createUpstreamTemplate()
    val copy = copyForOtherUser(upstream)
    val own =
        questionnaireService.create(
            OTHER_USERS_ROOM_ID, "Own ${UUID.randomUUID()}", QuestionnaireType.QUIZ)
    addQuestion(own.id!!)
    val derived = createTemplate(own, attribution = "Deriver")
    transactionTemplate.execute {
      contentMutationController.moveQuestionnaireContentToQuestionnaire(
          contentsOf(copy.id!!).first().id!!, own.id!!)
    }
    templateMutationController.updateTemplateFromSource(derived.id!!)

    Assertions.assertEquals(
        listOf(listOf(), listOf(upstream.id!! to "Someone")),
        upstreamOf(copyForCreator(derived).id!!))
  }

  private fun publish(template: Template, licenseId: String?): Template =
      templateMutationController.updateTemplate(
          UpdateTemplateInput(
              id = template.id!!,
              aiGenerated = null,
              attribution = null,
              description = null,
              language = null,
              licenseId = licenseId,
              name = null,
              published = true,
              tagNames = null))

  @Test
  fun shouldRequireALicenceToPublish() {
    val quiz = createQuiz(createRoom(creator).id!!, 1)
    Assertions.assertThrows(InvalidInputException::class.java) {
      createTemplate(quiz, licenseId = null)
    }
    val unlicensed = createTemplate(quiz, published = false, licenseId = null)
    Assertions.assertThrows(InvalidInputException::class.java) { publish(unlicensed, null) }
    val published = publish(unlicensed, "CC0-1.0")
    Assertions.assertEquals(
        listOf(true, "CC0-1.0"), listOf(published.published, published.license?.id))
  }

  @Test
  fun shouldRejectAPublishedTemplateWithoutLicenceInTheDatabase() {
    val template =
        createTemplate(createQuiz(createRoom(creator).id!!, 1), published = false, licenseId = null)
    template.published = true
    Assertions.assertThrows(DataIntegrityViolationException::class.java) {
      templateRepository.saveAndFlush(template)
    }
  }
}
