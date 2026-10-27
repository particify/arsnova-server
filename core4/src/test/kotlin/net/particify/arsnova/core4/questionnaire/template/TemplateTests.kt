/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.template

import java.util.Locale
import java.util.UUID
import net.particify.arsnova.core4.TestcontainersConfiguration
import net.particify.arsnova.core4.common.exception.InvalidInputException
import net.particify.arsnova.core4.questionnaire.answer.api.AnswerMutationController
import net.particify.arsnova.core4.questionnaire.answer.api.AnswerQueryController
import net.particify.arsnova.core4.questionnaire.answer.api.CreateChoiceAnswerInput
import net.particify.arsnova.core4.questionnaire.answer.api.LeaderboardQueryController
import net.particify.arsnova.core4.questionnaire.content.ChoiceContentFormatData
import net.particify.arsnova.core4.questionnaire.content.Content
import net.particify.arsnova.core4.questionnaire.content.ContentRepository
import net.particify.arsnova.core4.questionnaire.content.ContentServiceImpl
import net.particify.arsnova.core4.questionnaire.content.TextContentFormatData
import net.particify.arsnova.core4.questionnaire.content.WordcloudContentFormatData
import net.particify.arsnova.core4.questionnaire.content.api.ContentMutationController
import net.particify.arsnova.core4.questionnaire.content.api.ContentQueryController
import net.particify.arsnova.core4.questionnaire.content.api.TextContentMutationController
import net.particify.arsnova.core4.questionnaire.event.internal.TemplateEventHandler
import net.particify.arsnova.core4.questionnaire.exception.QuestionnaireWithNameAlreadyExistsException
import net.particify.arsnova.core4.questionnaire.exception.RoomlessQuestionnaireException
import net.particify.arsnova.core4.questionnaire.exception.TemplateSourceNotFoundException
import net.particify.arsnova.core4.questionnaire.questionnaire.Questionnaire
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnaireServiceImpl
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnaireType
import net.particify.arsnova.core4.questionnaire.questionnaire.api.QuestionnaireMutationController
import net.particify.arsnova.core4.questionnaire.questionnaire.api.QuestionnairePublishingMutationController
import net.particify.arsnova.core4.questionnaire.questionnaire.api.QuestionnaireQueryController
import net.particify.arsnova.core4.questionnaire.questionnaire.api.UpdateQuestionnaireInput
import net.particify.arsnova.core4.questionnaire.round.api.RoundMutationController
import net.particify.arsnova.core4.questionnaire.template.api.CreateQuestionnaireFromTemplateInput
import net.particify.arsnova.core4.questionnaire.template.api.CreateTemplateFromQuestionnaireInput
import net.particify.arsnova.core4.questionnaire.template.api.TemplateContentAttribution
import net.particify.arsnova.core4.questionnaire.template.api.TemplateFieldController
import net.particify.arsnova.core4.questionnaire.template.api.TemplateMutationController
import net.particify.arsnova.core4.questionnaire.template.api.TemplateQueryController
import net.particify.arsnova.core4.questionnaire.template.api.TemplateQueryInput
import net.particify.arsnova.core4.questionnaire.template.api.UpdateTemplateInput
import net.particify.arsnova.core4.room.Room
import net.particify.arsnova.core4.room.internal.RoomServiceImpl
import net.particify.arsnova.core4.user.User
import net.particify.arsnova.core4.user.UserService
import net.particify.arsnova.core4.user.event.UserDeletedEvent
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.data.domain.ScrollPosition
import org.springframework.data.repository.findByIdOrNull
import org.springframework.graphql.data.query.ScrollSubrange
import org.springframework.security.access.AccessDeniedException
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

@SpringBootTest(properties = [EXTERNALIZATION_PROPERTY])
@ActiveProfiles("test")
@Import(TestcontainersConfiguration::class)
class TemplateTests {
  @Autowired lateinit var answerMutationController: AnswerMutationController
  @Autowired lateinit var answerQueryController: AnswerQueryController
  @Autowired lateinit var contentMutationController: ContentMutationController
  @Autowired lateinit var contentQueryController: ContentQueryController
  @Autowired lateinit var contentRepository: ContentRepository
  @Autowired lateinit var contentService: ContentServiceImpl
  @Autowired lateinit var leaderboardQueryController: LeaderboardQueryController
  @Autowired lateinit var publishingController: QuestionnairePublishingMutationController
  @Autowired lateinit var questionnaireMutationController: QuestionnaireMutationController
  @Autowired lateinit var questionnaireQueryController: QuestionnaireQueryController
  @Autowired lateinit var questionnaireService: QuestionnaireServiceImpl
  @Autowired lateinit var roomService: RoomServiceImpl
  @Autowired lateinit var roundMutationController: RoundMutationController
  @Autowired lateinit var templateEventHandler: TemplateEventHandler
  @Autowired lateinit var templateFieldController: TemplateFieldController
  @Autowired lateinit var templateMutationController: TemplateMutationController
  @Autowired lateinit var templateQueryController: TemplateQueryController
  @Autowired lateinit var templateRepository: TemplateRepository
  @Autowired lateinit var templateTagRepository: TemplateTagRepository
  @Autowired lateinit var textContentMutationController: TextContentMutationController
  @Autowired lateinit var transactionTemplate: TransactionTemplate
  @Autowired lateinit var userService: UserService

  private val subrange = ScrollSubrange.create(ScrollPosition.offset(), null, true)

  private lateinit var creator: User
  private lateinit var source: Questionnaire
  private lateinit var sourceChoice: Content
  private lateinit var sourceWordcloud: Content
  private lateinit var sourceText: Content

  @BeforeEach
  fun createSource() {
    creator = authenticate(CREATOR_ID)
    source = createQuizWithContents(creator)
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

  private fun createQuizWithContents(user: User): Questionnaire {
    val room = createRoom(user)
    val quiz = questionnaireService.create(room.id!!, "Capitals", QuestionnaireType.QUIZ)
    sourceChoice =
        contentService.create(
            quiz.id!!,
            "Capital of France ${UUID.randomUUID()}",
            ChoiceContentFormatData(listOf("Berlin", "Paris"), listOf(1), false),
            null,
            true,
            null)
    sourceWordcloud =
        contentService.create(quiz.id!!, "Cities", WordcloudContentFormatData(), null, true, null)
    sourceText =
        contentService.create(quiz.id!!, "Why?", TextContentFormatData(false), null, true, null)
    contentService.banWordcloudKeyword(sourceWordcloud.id!!, "banned")
    contentService.updateTextAnswersPublished(sourceText.id!!, true)
    return questionnaireService.updatePublished(quiz.id!!, true)
  }

  private fun createTemplate(
      published: Boolean,
      questionnaire: Questionnaire = source,
      licenseId: String? = "CC-BY-4.0",
      attribution: String = "Someone"
  ): Template =
      templateMutationController.createTemplateFromQuestionnaire(
          CreateTemplateFromQuestionnaireInput(
              aiGenerated = false,
              attribution = attribution,
              description = "European capitals",
              language = "en",
              licenseId = licenseId,
              name = "Capitals template",
              published = published,
              questionnaireId = questionnaire.id!!,
              tagNames = listOf("Geography", " geography ", "Europe")))

  private fun contentsOf(questionnaireId: UUID): List<Content> =
      contentRepository
          .findByQuestionnaireIdOrderByPosition(questionnaireId, ScrollPosition.offset())
          .toList()

  private fun tagsOf(template: Template): List<TemplateTag> =
      templateFieldController.tags(listOf(template))[template].orEmpty()

  private fun sourceOf(template: Template, user: User): UUID? {
    val principal = UsernamePasswordAuthenticationToken(user, null, user.authorities)
    return templateFieldController.sourceQuestionnaireId(listOf(template), principal)[template]
  }

  private fun assertAllThrow(type: Class<out Throwable>, vararg calls: Executable) {
    calls.forEach { Assertions.assertThrows(type, it) }
  }

  @Test
  fun shouldCopyContentsIntoRoomlessQuestionnaireWithoutRuntimeState() {
    val template = createTemplate(published = false)
    val roomless = questionnaireService.findByIdOrNull(template.id!!)!!
    Assertions.assertNull(roomless.roomId)
    Assertions.assertFalse(roomless.published)
    Assertions.assertEquals(2, template.tags.size)
    Assertions.assertEquals("CC-BY-4.0", template.license?.id)
    val contents = contentsOf(template.id!!)
    Assertions.assertEquals(listOf(sourceChoice.body, "Cities", "Why?"), contents.map { it.body })
    Assertions.assertTrue(
        (contents[1].formatData as WordcloudContentFormatData).bannedKeywords.isEmpty())
    Assertions.assertFalse((contents[2].formatData as TextContentFormatData).answersPublished)
    // The source keeps its own state.
    Assertions.assertEquals(
        listOf("banned"),
        (contentRepository.findByIdOrNull(sourceWordcloud.id!!)!!.formatData
                as WordcloudContentFormatData)
            .bannedKeywords)
  }

  @Test
  fun shouldRejectPresentationOperationsOnTemplateContents() {
    val template = createTemplate(published = false)
    val questionnaireId = template.id!!
    val contents = contentsOf(questionnaireId)
    val contentId = contents.first().id!!
    assertAllThrow(
        RoomlessQuestionnaireException::class.java,
        { roundMutationController.startQuestionnaireContentRound(contentId) },
        { publishingController.startQuestionnaireContent(contentId) },
        { publishingController.stopQuestionnaireContent(questionnaireId) },
        { publishingController.publishQuestionnaireUpToContent(questionnaireId, contentId) },
        { questionnaireMutationController.toggleQuestionnairePublished(questionnaireId, true) },
        {
          questionnaireMutationController.updateQuestionnaire(
              UpdateQuestionnaireInput(questionnaireId, "Renamed", null, null, null, null, null))
        },
        {
          questionnaireMutationController.updateQuestionnaire(
              UpdateQuestionnaireInput(questionnaireId, null, true, null, null, null, null))
        },
        { questionnaireMutationController.duplicateQuestionnaire(questionnaireId, "Copy") },
        { questionnaireMutationController.resetQuestionnaireResults(questionnaireId) },
        {
          contentMutationController.copyQuestionnaireContentToQuestionnaire(contentId, source.id!!)
        },
        {
          contentMutationController.moveQuestionnaireContentToQuestionnaire(
              sourceChoice.id!!, questionnaireId)
        },
        {
          contentMutationController.banQuestionnaireWordcloudContentKeyword(
              contents[1].id!!, "word")
        },
        {
          contentMutationController.resetBannedQuestionnaireWordcloudContentKeywords(
              contents[1].id!!)
        },
        {
          textContentMutationController.toggleQuestionnaireTextContentAnswersPublished(
              contents[2].id!!, true)
        },
        {
          answerQueryController.questionnaireAnswerResultsByQuestionnaireId(
              questionnaireId, creator)
        },
        {
          answerQueryController.questionnaireContentAnswerResultsByQuestionnaireId(
              questionnaireId, creator)
        },
    )
  }

  @Test
  fun shouldLetCreatorEditTemplateSettingsAndContents() {
    val template = createTemplate(published = false)
    val questionnaireId = template.id!!
    questionnaireMutationController.updateQuestionnaire(
        UpdateQuestionnaireInput(questionnaireId, null, null, null, false, null, false))
    val content = contentsOf(questionnaireId).first()
    // A request keeps its session open for lazy loading; called directly, the test has to.
    transactionTemplate.execute {
      contentMutationController.duplicateQuestionnaireContent(content.id!!)
    }
    Assertions.assertEquals(4, contentsOf(questionnaireId).size)
    val updated =
        templateMutationController.updateTemplate(
            UpdateTemplateInput(
                questionnaireId, null, "", null, null, null, "Renamed", true, listOf("Maps")))
    Assertions.assertEquals("Renamed", questionnaireService.findByIdOrNull(questionnaireId)!!.name)
    Assertions.assertNull(updated.attribution)
    Assertions.assertTrue(updated.published)
    Assertions.assertEquals(listOf("Maps"), updated.tags.map { it.name })
  }

  @Test
  fun shouldListLicencesFromTheMostToTheLeastPermissive() {
    Assertions.assertEquals(
        listOf("CC0-1.0", "CC-BY-4.0", "CC-BY-SA-4.0", "CC-BY-NC-4.0", "CC-BY-NC-SA-4.0"),
        templateQueryController.templateLicenses().map { it.id })
  }

  @Test
  fun shouldKeepLicenceOnceSet() {
    val template = createTemplate(published = false)
    Assertions.assertThrows(InvalidInputException::class.java) {
      templateMutationController.updateTemplate(
          UpdateTemplateInput(template.id!!, null, null, null, null, "CC0-1.0", null, null, null))
    }
  }

  @Test
  fun shouldClearTheTagsOnANewLanguageWithoutTagNames() {
    val template = createTemplate(published = false)
    val updated =
        templateMutationController.updateTemplate(
            UpdateTemplateInput(template.id!!, null, null, null, "de", null, null, null, null))
    Assertions.assertEquals("de", updated.language)
    Assertions.assertEquals(listOf<TemplateTag>(), tagsOf(updated))
  }

  @Test
  fun shouldSetTheGivenTagsOnANewLanguage() {
    val template = createTemplate(published = false)
    val updated =
        templateMutationController.updateTemplate(
            UpdateTemplateInput(
                template.id!!, null, null, null, "de", null, null, null, listOf("Erdkunde")))
    val tags = tagsOf(updated)
    Assertions.assertEquals(listOf("Erdkunde"), tags.map { it.name })
    Assertions.assertEquals(listOf("de"), tags.map { it.language })
  }

  @Test
  fun shouldLeaveATagOfTheSameNameAloneOnInsertingItAgain() {
    val geography =
        tagsOf(createTemplate(published = false)).single {
          it.name.equals("geography", ignoreCase = true)
        }
    val inserted =
        transactionTemplate.execute {
          templateTagRepository.insertIfAbsent(UUID.randomUUID(), "en", "GEOGRAPHY")
        }
    Assertions.assertEquals(0, inserted)
    Assertions.assertEquals(
        geography.id, templateTagRepository.findByLanguageAndNameIgnoreCase("en", "geography")?.id)
  }

  private fun updateTexts(
      id: UUID,
      description: String? = null,
      attribution: String? = null,
      tagNames: List<String>? = null
  ): Template =
      templateMutationController.updateTemplate(
          UpdateTemplateInput(
              id = id,
              aiGenerated = null,
              attribution = attribution,
              description = description,
              language = null,
              licenseId = null,
              name = null,
              published = null,
              tagNames = tagNames))

  @Test
  fun shouldRejectTooLongTextsAndTooManyTags() {
    val id = createTemplate(published = false).id!!
    val tooManyTags = List(21) { "Tag $it" }
    assertAllThrow(
        InvalidInputException::class.java,
        { updateTexts(id, description = "d".repeat(2001)) },
        { updateTexts(id, attribution = "a".repeat(501)) },
        { updateTexts(id, tagNames = tooManyTags) },
        {
          templateMutationController.createTemplateFromQuestionnaire(
              CreateTemplateFromQuestionnaireInput(
                  attribution = "a".repeat(501),
                  description = null,
                  language = "en",
                  licenseId = null,
                  name = "Too long",
                  questionnaireId = source.id!!,
                  tagNames = null))
        },
        {
          templateMutationController.createTemplateFromQuestionnaire(
              CreateTemplateFromQuestionnaireInput(
                  attribution = null,
                  description = null,
                  language = "en",
                  licenseId = null,
                  name = "Too many tags",
                  questionnaireId = source.id!!,
                  tagNames = tooManyTags))
        },
    )
  }

  @Test
  fun shouldAcceptTextsAndTagsUpToTheLimitsOnceTrimmed() {
    val id = createTemplate(published = false).id!!
    val description = "d".repeat(2000)
    val updated =
        updateTexts(
            id,
            description = "  $description\n",
            attribution = " Someone else ",
            tagNames = List(20) { "Tag $it" })
    Assertions.assertEquals(description, updated.description)
    Assertions.assertEquals("Someone else", updated.attribution)
    Assertions.assertEquals(20, tagsOf(updated).size)
    Assertions.assertNull(updateTexts(id, description = "   ").description)
  }

  @Test
  fun shouldListTheTemplateWithTheLatestContentEditFirst() {
    val edited = createTemplate(published = false)
    val newer = createTemplate(published = false, questionnaire = createQuizWithContents(creator))
    val content = contentsOf(edited.id!!)[1]
    contentService.update(content.id!!, "Towns", WordcloudContentFormatData(), null, true, null)
    Assertions.assertEquals(
        listOf(edited.id, newer.id),
        templateQueryController.currentUserTemplates(creator, subrange).content.take(2).map {
          it.id
        })
  }

  @Test
  fun shouldLetOnlyTheCreatorListTheTemplateContents() {
    val template = createTemplate(published = false)
    val questionnaireId = template.id!!
    val listed = contentQueryController.questionnaireContentsByQuestionnaireId(questionnaireId)
    Assertions.assertEquals(contentsOf(questionnaireId).map { it.id }, listed.content.map { it.id })
    authenticate(OTHER_USER_ID)
    Assertions.assertThrows(AccessDeniedException::class.java) {
      contentQueryController.questionnaireContentsByQuestionnaireId(questionnaireId)
    }
  }

  @Test
  fun shouldHideUnpublishedTemplateFromOthers() {
    val template = createTemplate(published = false)
    val questionnaireId = template.id!!
    authenticate(OTHER_USER_ID)
    assertAllThrow(
        AccessDeniedException::class.java,
        { templateQueryController.templateById(questionnaireId) },
        { questionnaireQueryController.questionnaireById(questionnaireId) },
        { contentQueryController.questionnairePublishedContentsByQuestionnaireId(questionnaireId) },
        {
          templateMutationController.createQuestionnaireFromTemplate(
              CreateQuestionnaireFromTemplateInput(null, OTHER_USERS_ROOM_ID, questionnaireId))
        },
    )
  }

  @Test
  fun shouldLetOthersReadButNotChangeOrPresentPublishedTemplate() {
    val template = createTemplate(published = true)
    val questionnaireId = template.id!!
    val contents = contentsOf(questionnaireId)
    authenticate(OTHER_USER_ID)
    Assertions.assertNotNull(templateQueryController.templateById(questionnaireId))
    Assertions.assertEquals(
        3,
        contentQueryController
            .questionnairePublishedContentsByQuestionnaireId(questionnaireId)
            .size())
    val otherUser = userService.loadUserById(OTHER_USER_ID)!!
    Assertions.assertNull(sourceOf(template, otherUser))
    assertAllThrow(
        AccessDeniedException::class.java,
        {
          templateMutationController.updateTemplate(
              UpdateTemplateInput(questionnaireId, null, null, null, null, null, "X", null, null))
        },
        { templateMutationController.deleteTemplate(questionnaireId) },
        { templateMutationController.updateTemplateFromSource(questionnaireId) },
        { contentQueryController.questionnaireContentsByQuestionnaireId(questionnaireId) },
        { contentMutationController.deleteQuestionnaireContent(contents.first().id!!) },
        {
          answerMutationController.createQuestionnaireChoiceAnswer(
              CreateChoiceAnswerInput(contents.first().activeRound!!.id!!, listOf(1)))
        },
        { roundMutationController.startQuestionnaireContentRound(contents.first().id!!) },
        {
          leaderboardQueryController.questionnaireLeaderboardByQuestionnaireId(
              questionnaireId, null, null, otherUser, Locale.ENGLISH)
        },
        {
          contentMutationController.copyQuestionnaireContentToQuestionnaire(
              contents.first().id!!,
              questionnaireService
                  .create(OTHER_USERS_ROOM_ID, "Own ${UUID.randomUUID()}", QuestionnaireType.QUIZ)
                  .id!!)
        },
    )
    // Even the creator cannot take single contents out of a template.
    authenticate(CREATOR_ID)
    Assertions.assertThrows(RoomlessQuestionnaireException::class.java) {
      contentMutationController.copyQuestionnaireContentToQuestionnaire(
          contents.first().id!!, source.id!!)
    }
  }

  @Test
  fun shouldCreateUnpublishedQuestionnaireWithProvenanceFromTemplate() {
    val template = createTemplate(published = true)
    val templateContents = contentsOf(template.id!!)
    authenticate(OTHER_USER_ID)
    val copy =
        templateMutationController.createQuestionnaireFromTemplate(
            CreateQuestionnaireFromTemplateInput(null, OTHER_USERS_ROOM_ID, template.id!!))
    Assertions.assertEquals(OTHER_USERS_ROOM_ID, copy.roomId)
    // Defaults to the template's name, which is why a second copy collides.
    Assertions.assertEquals("Capitals template", copy.name)
    Assertions.assertFalse(copy.published)
    Assertions.assertEquals(template.id, copy.sourceTemplateId)
    val copiedContents = contentsOf(copy.id!!)
    Assertions.assertEquals(
        templateContents.map { it.id }, copiedContents.map { it.sourceContentId })
    copiedContents.forEach {
      Assertions.assertEquals(
          listOf(template.id, "CC-BY-4.0", "Someone"),
          listOf(it.sourceTemplateId, it.sourceTemplateLicenseId, it.sourceTemplateAttribution))
    }
    Assertions.assertThrows(QuestionnaireWithNameAlreadyExistsException::class.java) {
      templateMutationController.createQuestionnaireFromTemplate(
          CreateQuestionnaireFromTemplateInput(null, OTHER_USERS_ROOM_ID, template.id!!))
    }
  }

  @Test
  fun shouldNotCarryProvenanceIntoATemplateMadeFromACopy() {
    val template = createTemplate(published = true)
    authenticate(OTHER_USER_ID)
    val copy =
        templateMutationController.createQuestionnaireFromTemplate(
            CreateQuestionnaireFromTemplateInput(
                "Copy ${UUID.randomUUID()}", OTHER_USERS_ROOM_ID, template.id!!))
    val templateOfCopy = createTemplate(published = false, questionnaire = copy)
    val roomless = questionnaireService.findByIdOrNull(templateOfCopy.id!!)!!
    Assertions.assertNull(roomless.sourceTemplateId)
    contentsOf(templateOfCopy.id!!).forEach {
      Assertions.assertNull(it.sourceTemplateId)
      Assertions.assertNull(it.sourceContentId)
      Assertions.assertNull(it.sourceTemplateAttribution)
    }
  }

  private fun copyForOtherUser(template: Template): Questionnaire {
    authenticate(OTHER_USER_ID)
    return templateMutationController.createQuestionnaireFromTemplate(
        CreateQuestionnaireFromTemplateInput(
            "Copy ${UUID.randomUUID()}", OTHER_USERS_ROOM_ID, template.id!!))
  }

  @Test
  fun shouldKeepTheLicenceOfACopyInATemplateMadeFromIt() {
    val copy = copyForOtherUser(createTemplate(published = true))
    listOf(null, "CC0-1.0").forEach {
      Assertions.assertThrows(InvalidInputException::class.java) {
        createTemplate(published = true, questionnaire = copy, licenseId = it)
      }
    }
    Assertions.assertEquals(
        "CC-BY-4.0", createTemplate(published = true, questionnaire = copy).license?.id)
  }

  @Test
  fun shouldKeepTheLicenceOfContentsMovedOutOfACopy() {
    val copy = copyForOtherUser(createTemplate(published = true))
    val own =
        questionnaireService.create(
            OTHER_USERS_ROOM_ID, "Own ${UUID.randomUUID()}", QuestionnaireType.QUIZ)
    val unlicensed = createTemplate(published = false, questionnaire = own, licenseId = null)
    transactionTemplate.execute {
      contentMutationController.moveQuestionnaireContentToQuestionnaire(
          contentsOf(copy.id!!).first().id!!, own.id!!)
    }
    assertAllThrow(
        InvalidInputException::class.java,
        { createTemplate(published = true, questionnaire = own, licenseId = "CC0-1.0") },
        { templateMutationController.updateTemplateFromSource(unlicensed.id!!) },
    )
    val attribution =
        templateQueryController.templateContentAttributionsByQuestionnaireId(own.id!!).single()
    Assertions.assertEquals("CC-BY-4.0", attribution.license?.id)
  }

  @Test
  fun shouldKeepTheAttributionOfContentsMovedOutOfACopy() {
    val copy = copyForOtherUser(createTemplate(published = true))
    authenticate(creator)
    val other = copyForOtherUser(createTemplate(published = true, attribution = "Someone else"))
    val own =
        questionnaireService.create(
            OTHER_USERS_ROOM_ID, "Own ${UUID.randomUUID()}", QuestionnaireType.QUIZ)
    transactionTemplate.execute {
      contentMutationController.moveQuestionnaireContentToQuestionnaire(
          contentsOf(copy.id!!).first().id!!, own.id!!)
      contentMutationController.moveQuestionnaireContentToQuestionnaire(
          contentsOf(copy.id!!).first().id!!, other.id!!)
    }
    Assertions.assertEquals(
        listOf("Someone"),
        templateQueryController.templateContentAttributionsByQuestionnaireId(own.id!!).map {
          it.attribution
        })
    Assertions.assertEquals(
        listOf("Someone else", "Someone else", "Someone else", "Someone"),
        templateQueryController.templateContentAttributionsByQuestionnaireId(other.id!!).map {
          it.attribution
        })
  }

  @Test
  fun shouldAttributeEveryContentOfACopyToItsTemplate() {
    val template = createTemplate(published = true)
    val copy = copyForOtherUser(template)
    val attributions =
        templateQueryController.templateContentAttributionsByQuestionnaireId(copy.id!!)
    Assertions.assertEquals(contentsOf(copy.id!!).map { it.id }, attributions.map { it.contentId })
    attributions.forEach {
      Assertions.assertEquals(template.id, it.templateId)
      Assertions.assertEquals("Someone", it.attribution)
      Assertions.assertEquals("CC-BY-4.0", it.license?.id)
    }

    authenticate(creator)
    templateMutationController.deleteTemplate(template.id!!)
    authenticate(OTHER_USER_ID)
    // Licences are entities without structural equality, hence the comparison by id.
    Assertions.assertEquals(
        attributions.map { it.copy(license = null) to it.license?.id },
        templateQueryController.templateContentAttributionsByQuestionnaireId(copy.id!!).map {
          it.copy(license = null) to it.license?.id
        })
  }

  @Test
  fun shouldAttributeNothingInAQuestionnaireNotCopiedFromATemplate() {
    Assertions.assertEquals(
        listOf<TemplateContentAttribution>(),
        templateQueryController.templateContentAttributionsByQuestionnaireId(source.id!!))
  }

  @Test
  fun shouldDenyAttributionsToUsersWhoMayNotReadTheQuestionnaire() {
    val copy = copyForOtherUser(createTemplate(published = true))
    authenticate(userService.createAccount())
    Assertions.assertThrows(AccessDeniedException::class.java) {
      templateQueryController.templateContentAttributionsByQuestionnaireId(copy.id!!)
    }
  }

  @Test
  fun shouldReplaceContentsAndSettingsFromSource() {
    val template = createTemplate(published = false)
    contentMutationController.deleteQuestionnaireContent(contentsOf(template.id!!).first().id!!)
    contentService.create(
        source.id!!,
        "Capital of Italy",
        ChoiceContentFormatData(listOf("Rome", "Milan"), listOf(0), false),
        null,
        true,
        null)
    questionnaireService.update(source.id!!, null, null, null, null, null, false)
    templateMutationController.updateTemplateFromSource(template.id!!)
    val contents = contentsOf(template.id!!)
    Assertions.assertEquals(4, contents.size)
    Assertions.assertEquals("Capital of Italy", contents.last().body)
    Assertions.assertEquals(
        false, questionnaireService.findByIdOrNull(template.id!!)!!.leaderboardEnabled)
    questionnaireMutationController.deleteQuestionnaire(source.id!!)
    Assertions.assertNull(sourceOf(template, creator))
    Assertions.assertThrows(TemplateSourceNotFoundException::class.java) {
      templateMutationController.updateTemplateFromSource(template.id!!)
    }
  }

  @Test
  fun shouldFindPublishedTemplatesByContentText() {
    val template = createTemplate(published = true)
    val search = sourceChoice.body!!.substringAfter("France ")
    val unpublished =
        createTemplate(published = false, questionnaire = createQuizWithContents(creator))
    val found =
        templateQueryController
            .publishedTemplates(TemplateQueryInput(language = "en", search = search), subrange)
            .content
            .map { it.id }
    Assertions.assertTrue(found.contains(template.id))
    Assertions.assertFalse(found.contains(unpublished.id))
    val literal =
        templateQueryController
            .publishedTemplates(TemplateQueryInput(search = "%"), subrange)
            .content
            .map { it.id }
    Assertions.assertFalse(literal.contains(template.id))
  }

  @Test
  fun shouldDeleteUnpublishedTemplatesButKeepPublishedOnesAndCopiesWithUser() {
    val user = authenticate(userService.createAccount())
    val quiz = createQuizWithContents(user)
    val published = createTemplate(published = true, questionnaire = quiz)
    val unpublished = createTemplate(published = false, questionnaire = quiz)
    authenticate(OTHER_USER_ID)
    val copy =
        templateMutationController.createQuestionnaireFromTemplate(
            CreateQuestionnaireFromTemplateInput(
                "Kept ${UUID.randomUUID()}", OTHER_USERS_ROOM_ID, published.id!!))
    // Only the template module's handler: room deletion is covered elsewhere.
    templateEventHandler.handleUserDeleted(UserDeletedEvent(user.id!!))
    Assertions.assertNull(templateRepository.findByIdOrNull(unpublished.id!!))
    Assertions.assertNull(questionnaireService.findByIdOrNull(unpublished.id!!))
    Assertions.assertNotNull(templateRepository.findByIdOrNull(published.id!!))
    Assertions.assertEquals(3, contentsOf(published.id!!).size)
    Assertions.assertNotNull(questionnaireService.findByIdOrNull(copy.id!!))
  }
}
