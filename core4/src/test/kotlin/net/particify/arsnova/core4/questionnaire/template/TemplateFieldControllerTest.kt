/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.template

import graphql.GraphQLContext
import java.util.UUID
import net.particify.arsnova.core4.common.AuditMetadata
import net.particify.arsnova.core4.questionnaire.template.api.TemplateFieldController
import net.particify.arsnova.core4.user.User
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken

private const val PAGE_SIZE = 100

class TemplateFieldControllerTest {
  private val templatingService = Mockito.mock(TemplateQuestionnaireService::class.java)
  private val tagRepository = Mockito.mock(TemplateTagRepository::class.java)
  private val controller =
      TemplateFieldController(TemplateTagServiceImpl(tagRepository), templatingService)
  private val creator = User(id = UUID.randomUUID())
  private val templates =
      List(PAGE_SIZE) {
        Template(
            id = UUID.randomUUID(),
            sourceQuestionnaireId = UUID.randomUUID(),
            auditMetadata = AuditMetadata(createdBy = creator.id))
      }

  init {
    Mockito.`when`(templatingService.findSummaries(Mockito.anyCollection())).thenAnswer {
      @Suppress("UNCHECKED_CAST")
      (it.arguments[0] as Collection<UUID>).associateWith { id ->
        QuestionnaireSummary(id, "QUIZ", 2)
      }
    }
    Mockito.`when`(templatingService.findRoomQuestionnaireIds(Mockito.anyCollection())).thenAnswer {
      @Suppress("UNCHECKED_CAST") (it.arguments[0] as Collection<UUID>).toSet()
    }
  }

  @Test
  fun shouldResolveAPageOfTemplatesWithOneLookupPerKind() {
    val tag = TemplateTag(id = UUID.randomUUID(), language = "en", name = "Maps")
    Mockito.`when`(tagRepository.findWithTemplateIdByTemplateIds(Mockito.anyCollection()))
        .thenReturn(listOf(arrayOf(templates.first().id!!, tag)))
    val context = GraphQLContext.newContext().build()
    val principal = UsernamePasswordAuthenticationToken(creator, null, listOf())

    val counts = controller.contentCount(templates, context)
    val types = controller.questionnaireType(templates, context)
    val tags = controller.tags(templates)
    val sources = controller.sourceQuestionnaireId(templates, principal)

    Assertions.assertEquals(templates.map { 2 }, templates.map { counts[it] })
    Assertions.assertEquals(templates.map { "QUIZ" }, templates.map { types[it] })
    Assertions.assertEquals(listOf(tag), tags[templates.first()])
    Assertions.assertEquals(listOf<TemplateTag>(), tags[templates.last()])
    Assertions.assertEquals(
        templates.map { it.sourceQuestionnaireId }, templates.map { sources[it] })
    Mockito.verify(templatingService).findSummaries(Mockito.anyCollection())
    Mockito.verify(templatingService).findRoomQuestionnaireIds(Mockito.anyCollection())
    Mockito.verify(tagRepository).findWithTemplateIdByTemplateIds(Mockito.anyCollection())
    Mockito.verifyNoMoreInteractions(templatingService, tagRepository)
  }

  @Test
  fun shouldHideTheSourceFromOthersWithoutLookingItUp() {
    val other = User(id = UUID.randomUUID())
    val principal = UsernamePasswordAuthenticationToken(other, null, listOf())
    Assertions.assertEquals(
        mapOf<Template, UUID>(), controller.sourceQuestionnaireId(templates, principal))
    Assertions.assertEquals(
        mapOf<Template, UUID>(), controller.sourceQuestionnaireId(templates, null))
    Mockito.verify(templatingService, Mockito.never())
        .findRoomQuestionnaireIds(Mockito.anyCollection())
  }
}
