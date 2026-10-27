/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.template.api

import com.querydsl.core.BooleanBuilder
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import java.util.UUID
import net.particify.arsnova.core4.license.License
import net.particify.arsnova.core4.license.LicenseService
import net.particify.arsnova.core4.questionnaire.exception.TemplateNotFoundException
import net.particify.arsnova.core4.questionnaire.template.QTemplate
import net.particify.arsnova.core4.questionnaire.template.Template
import net.particify.arsnova.core4.questionnaire.template.TemplateQuestionnaireService
import net.particify.arsnova.core4.questionnaire.template.TemplateServiceImpl
import net.particify.arsnova.core4.questionnaire.template.TemplateTag
import net.particify.arsnova.core4.questionnaire.template.TemplateTagServiceImpl
import net.particify.arsnova.core4.user.User
import org.springframework.data.domain.OffsetScrollPosition
import org.springframework.data.domain.ScrollPosition
import org.springframework.data.domain.Sort
import org.springframework.data.domain.Window
import org.springframework.data.repository.findByIdOrNull
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.graphql.data.query.ScrollSubrange
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller

@Controller
@PreAuthorize("hasRole('USER')")
@SchemaMapping(typeName = "Query")
class TemplateQueryController(
    private val templateService: TemplateServiceImpl,
    private val templateTagService: TemplateTagServiceImpl,
    private val templateQuestionnaireService: TemplateQuestionnaireService,
    private val licenseService: LicenseService,
    private val entityManager: EntityManager,
) {
  companion object {
    const val DEFAULT_QUERY_LIMIT = 50
  }

  @QueryMapping
  fun currentUserTemplates(
      @AuthenticationPrincipal user: User,
      subrange: ScrollSubrange
  ): Window<Template> {
    val position =
        subrange.position().orElse(ScrollPosition.offset()) as? OffsetScrollPosition
            ?: ScrollPosition.offset()
    // Positions point at the last element seen, so a page starts one after the given one.
    val skipped = if (position.isInitial) 0L else position.offset + 1
    val limit = DEFAULT_QUERY_LIMIT
    val qTemplate = QTemplate.template
    val lastChangedAt =
        qTemplate.auditMetadata.updatedAt.coalesce(qTemplate.auditMetadata.createdAt)
    val templates =
        JPAQueryFactory(entityManager)
            .selectFrom(qTemplate)
            .where(qTemplate.auditMetadata.createdBy.eq(user.id))
            .orderBy(lastChangedAt.desc(), qTemplate.id.desc())
            .offset(skipped)
            .limit(limit + 1L)
            .fetch()
    return Window.from(templates.take(limit), position.positionFunction(), templates.size > limit)
  }

  @QueryMapping
  fun publishedTemplates(
      @Argument query: TemplateQueryInput?,
      subrange: ScrollSubrange
  ): Window<Template> {
    val builder = buildPublishedFilter(query ?: TemplateQueryInput())
    return templateService.findBy(builder) { q ->
      q.sortBy(Sort.by("auditMetadata.createdAt", "id").descending())
          .limit(DEFAULT_QUERY_LIMIT)
          .scroll(subrange.position().orElse(ScrollPosition.offset()))
    }
  }

  private fun buildPublishedFilter(query: TemplateQueryInput): BooleanBuilder {
    val qTemplate = QTemplate.template
    val builder = BooleanBuilder(qTemplate.published.isTrue)
    query.language?.let { builder.and(qTemplate.language.eq(it)) }
    query.tagIds?.forEach { builder.and(qTemplate.tags.any().id.eq(it)) }
    query.search
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?.let {
          val textMatch = BooleanBuilder(qTemplate.name.containsIgnoreCase(it))
          textMatch.or(qTemplate.description.containsIgnoreCase(it))
          val contentMatches = templateQuestionnaireService.findRoomlessIdsMatchingText(it)
          if (contentMatches.isNotEmpty()) textMatch.or(qTemplate.id.`in`(contentMatches))
          builder.and(textMatch)
        }
    return builder
  }

  @QueryMapping
  @PreAuthorize("hasPermission(#id, 'Template', 'read')")
  fun templateById(@Argument id: UUID): Template {
    return templateService.findByIdOrNull(id) ?: throw TemplateNotFoundException(id)
  }

  /**
   * Reads the snapshot each content took when it was copied, so the credits survive the template
   * being unpublished or deleted, and go along when the content moves to another questionnaire.
   * Licences are never deleted. Each content reports the licence it was copied under, which is also
   * the one a template made from it has to keep.
   */
  @QueryMapping
  @PreAuthorize("hasPermission(#questionnaireId, 'Questionnaire', 'read')")
  fun templateContentAttributionsByQuestionnaireId(
      @Argument questionnaireId: UUID
  ): List<TemplateContentAttribution> {
    val contents = templateQuestionnaireService.findContentsCopiedFromTemplates(questionnaireId)
    val licenseIds =
        contents.flatMap { content ->
          listOf(content.sourceTemplateLicenseId) +
              content.upstreamAttributions.map { it.licenseId }
        }
    val licenses =
        licenseService.findAllById(licenseIds.filterNotNull().toSet()).associateBy { it.id }
    return contents.map { content ->
      TemplateContentAttribution(
          content.sourceTemplateAttribution,
          content.id!!,
          licenses[content.sourceTemplateLicenseId],
          content.sourceTemplateId!!,
          content.upstreamAttributions.map {
            TemplateUpstreamAttribution(it.attribution, licenses[it.licenseId], it.templateId)
          })
    }
  }

  @QueryMapping
  fun templateLicenses(): List<License> {
    return templateService.findSelectableLicenses()
  }

  /**
   * [verified] is a filter switch rather than a value to match: `true` lists only verified tags,
   * `false` lists all tags, verified ones included, and is reserved to admins.
   */
  @QueryMapping
  @PreAuthorize("hasRole('USER') and (#verified or hasRole('ADMIN'))")
  fun templateTagsByLanguage(
      @Argument language: String,
      @Argument verified: Boolean
  ): List<TemplateTag> {
    return templateTagService.findByLanguage(language, verified)
  }
}
