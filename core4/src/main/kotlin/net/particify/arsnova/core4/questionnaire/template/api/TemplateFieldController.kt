/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.template.api

import graphql.GraphQLContext
import java.security.Principal
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import net.particify.arsnova.core4.questionnaire.template.QuestionnaireSummary
import net.particify.arsnova.core4.questionnaire.template.Template
import net.particify.arsnova.core4.questionnaire.template.TemplateQuestionnaireService
import net.particify.arsnova.core4.questionnaire.template.TemplateTag
import net.particify.arsnova.core4.questionnaire.template.TemplateTagServiceImpl
import net.particify.arsnova.core4.user.User
import org.springframework.graphql.data.method.annotation.BatchMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.security.core.Authentication
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller

private const val SUMMARIES_CONTEXT_KEY = "templateQuestionnaireSummaries"

/** Fields needing further lookups are batched, so a page of templates costs a few statements. */
@Controller
class TemplateFieldController(
    private val templateTagService: TemplateTagServiceImpl,
    private val templateQuestionnaireService: TemplateQuestionnaireService,
) {
  @SchemaMapping(typeName = "Template")
  fun createdAt(template: Template): OffsetDateTime? {
    return template.auditMetadata.createdAt?.atOffset(ZoneOffset.UTC)
  }

  @SchemaMapping(typeName = "Template")
  fun updatedAt(template: Template): OffsetDateTime? {
    return template.auditMetadata.updatedAt?.atOffset(ZoneOffset.UTC)
  }

  @SchemaMapping(typeName = "Template")
  fun createdByCurrentUser(template: Template, @AuthenticationPrincipal user: User): Boolean {
    return isCreator(template, user.id)
  }

  @SchemaMapping(typeName = "Template")
  fun questionnaireId(template: Template): UUID {
    return template.id!!
  }

  /**
   * Only the creator sees the source, and only while it still exists. Templates left out of the
   * result resolve to null.
   */
  @BatchMapping(typeName = "Template")
  fun sourceQuestionnaireId(templates: List<Template>, principal: Principal?): Map<Template, UUID> {
    val userId = ((principal as? Authentication)?.principal as? User)?.id
    val sources =
        templates
            .filter { isCreator(it, userId) }
            .mapNotNull { template -> template.sourceQuestionnaireId?.let { template to it } }
    if (sources.isEmpty()) return emptyMap()
    val available = templateQuestionnaireService.findRoomQuestionnaireIds(sources.map { it.second })
    return sources.filter { it.second in available }.toMap()
  }

  @BatchMapping(typeName = "Template")
  fun tags(templates: List<Template>): Map<Template, List<TemplateTag>> {
    val tags = templateTagService.findByTemplateIds(templates.mapNotNull { it.id })
    return templates.associateWith { tags[it.id].orEmpty() }
  }

  @BatchMapping(typeName = "Template")
  fun contentCount(templates: List<Template>, context: GraphQLContext): Map<Template, Int> {
    val summaries = findSummaries(templates, context)
    return templates.associateWith { summaries[it.id]?.contentCount ?: 0 }
  }

  @BatchMapping(typeName = "Template")
  fun questionnaireType(templates: List<Template>, context: GraphQLContext): Map<Template, String> {
    val summaries = findSummaries(templates, context)
    return templates.associateWith { summaries.getValue(it.id!!).type }
  }

  /**
   * Shares the summaries between the fields which need them for the same templates, within one
   * request.
   */
  private fun findSummaries(
      templates: List<Template>,
      context: GraphQLContext
  ): Map<UUID, QuestionnaireSummary> {
    val cache: MutableMap<UUID, QuestionnaireSummary> =
        context.computeIfAbsent(SUMMARIES_CONTEXT_KEY) {
          ConcurrentHashMap<UUID, QuestionnaireSummary>()
        }
    val missing = templates.mapNotNull { it.id }.filterNot { cache.containsKey(it) }
    if (missing.isNotEmpty()) cache.putAll(templateQuestionnaireService.findSummaries(missing))
    return cache
  }

  private fun isCreator(template: Template, userId: UUID?): Boolean =
      userId != null && template.auditMetadata.createdBy == userId
}
