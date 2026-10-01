/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.template

import java.util.UUID
import net.particify.arsnova.core4.TestcontainersConfiguration
import net.particify.arsnova.core4.questionnaire.content.ChoiceContentFormatData
import net.particify.arsnova.core4.questionnaire.content.ContentServiceImpl
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnaireServiceImpl
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnaireType
import net.particify.arsnova.core4.questionnaire.template.api.AdminTemplateMutationController
import net.particify.arsnova.core4.questionnaire.template.api.CreateTemplateFromQuestionnaireInput
import net.particify.arsnova.core4.questionnaire.template.api.TemplateMutationController
import net.particify.arsnova.core4.questionnaire.template.api.TemplateQueryController
import net.particify.arsnova.core4.room.Room
import net.particify.arsnova.core4.room.internal.RoomServiceImpl
import net.particify.arsnova.core4.user.UserService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.ActiveProfiles

/** Accounts from the `dev` Liquibase fixtures, neither of them an admin. */
private val CREATOR_ID = UUID.fromString("9c778494-9b52-424d-8c7f-f4936629facb")
private val OTHER_USER_ID = UUID.fromString("43389844-708b-469d-8d22-abdd2eb88777")

/** Room events reach RabbitMQ, which none of this depends on. */
private const val EXTERNALIZATION_PROPERTY = "spring.modulith.events.externalization.enabled=false"

/** Unverified tags are listed to admins only, who also manage the tags and templates of others. */
@SpringBootTest(properties = [EXTERNALIZATION_PROPERTY])
@ActiveProfiles("test")
@Import(TestcontainersConfiguration::class)
class TemplateAdministrationTests {
  @Autowired lateinit var adminTemplateMutationController: AdminTemplateMutationController
  @Autowired lateinit var contentService: ContentServiceImpl
  @Autowired lateinit var questionnaireService: QuestionnaireServiceImpl
  @Autowired lateinit var roomService: RoomServiceImpl
  @Autowired lateinit var templateMutationController: TemplateMutationController
  @Autowired lateinit var templateQueryController: TemplateQueryController
  @Autowired lateinit var templateTagRepository: TemplateTagRepository
  @Autowired lateinit var userService: UserService

  @AfterEach
  fun clearAuthentication() {
    SecurityContextHolder.clearContext()
  }

  private fun authenticate(userId: UUID, admin: Boolean = false) {
    val user = userService.loadUserById(userId)!!
    val authorities =
        if (admin) user.authorities + SimpleGrantedAuthority("ROLE_ADMIN") else user.authorities
    SecurityContextHolder.getContext().authentication =
        UsernamePasswordAuthenticationToken(user, null, authorities)
  }

  private fun createPublishedTemplate(): Template {
    authenticate(CREATOR_ID)
    val user = userService.loadUserById(CREATOR_ID)!!
    val room =
        roomService.create(Room(shortId = roomService.generateShortId(), name = "Admin"), user)
    val questionnaire = questionnaireService.create(room.id!!, "Admin", QuestionnaireType.SURVEY)
    contentService.create(
        questionnaire.id!!,
        "Pick one",
        ChoiceContentFormatData(listOf("A", "B"), null, false),
        null,
        true,
        null)
    return templateMutationController.createTemplateFromQuestionnaire(
        CreateTemplateFromQuestionnaireInput(
            aiGenerated = false,
            attribution = null,
            description = null,
            language = "en",
            licenseId = "CC0-1.0",
            name = "Admin template",
            published = true,
            questionnaireId = questionnaire.id!!,
            tagNames = listOf()))
  }

  /** A language of its own keeps the verification from reaching tags of other tests. */
  private fun createUnverifiedTag(): TemplateTag {
    val language = "t" + UUID.randomUUID().toString().take(7)
    return templateTagRepository.save(TemplateTag(language = language, name = "Unverified"))
  }

  @Test
  fun shouldDenyUnverifiedTagsAndTheAdministrationToOthers() {
    val tag = createUnverifiedTag()
    val template = createPublishedTemplate()
    authenticate(OTHER_USER_ID)

    Assertions.assertEquals(
        listOf<TemplateTag>(), templateQueryController.templateTagsByLanguage(tag.language!!, true))
    listOf<() -> Any?>(
            { templateQueryController.templateTagsByLanguage(tag.language!!, false) },
            { adminTemplateMutationController.adminUpdateTemplateTagVerified(tag.id!!, true) },
            { adminTemplateMutationController.adminUpdateTemplateTagName(tag.id!!, "Renamed") },
            { adminTemplateMutationController.adminDeleteTemplateTag(tag.id!!) },
            { adminTemplateMutationController.adminDeleteTemplate(template.id!!) })
        .forEach { call -> Assertions.assertThrows(AccessDeniedException::class.java) { call() } }
  }

  @Test
  fun shouldLetAdminsListVerifyAndDeleteTagsAndTemplates() {
    val tag = createUnverifiedTag()
    val template = createPublishedTemplate()
    authenticate(OTHER_USER_ID, admin = true)

    Assertions.assertEquals(
        listOf(tag.id),
        templateQueryController.templateTagsByLanguage(tag.language!!, false).map { it.id })
    Assertions.assertTrue(
        adminTemplateMutationController.adminUpdateTemplateTagVerified(tag.id!!, true).verified)
    authenticate(OTHER_USER_ID)
    Assertions.assertEquals(
        listOf(tag.id),
        templateQueryController.templateTagsByLanguage(tag.language!!, true).map { it.id })
    authenticate(OTHER_USER_ID, admin = true)
    Assertions.assertEquals(
        tag.id, adminTemplateMutationController.adminDeleteTemplateTag(tag.id!!))
    Assertions.assertEquals(
        template.id, adminTemplateMutationController.adminDeleteTemplate(template.id!!))
  }
}
