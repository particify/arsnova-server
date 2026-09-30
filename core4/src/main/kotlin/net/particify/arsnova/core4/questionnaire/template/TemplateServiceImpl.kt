/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.template

import jakarta.transaction.Transactional
import java.time.Instant
import java.util.UUID
import net.particify.arsnova.core4.common.exception.AccessDeniedException
import net.particify.arsnova.core4.common.exception.InvalidInputException
import net.particify.arsnova.core4.license.License
import net.particify.arsnova.core4.license.LicenseService
import net.particify.arsnova.core4.questionnaire.exception.TemplateNotFoundException
import net.particify.arsnova.core4.questionnaire.exception.TemplateSourceNotFoundException
import net.particify.arsnova.core4.questionnaire.template.api.CreateTemplateFromQuestionnaireInput
import net.particify.arsnova.core4.questionnaire.template.api.UpdateTemplateInput
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.access.PermissionEvaluator
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service

// Bounded by the name column of the questionnaire which holds the contents.
private const val MAX_NAME_LENGTH = 50
private const val MAX_DESCRIPTION_LENGTH = 2000
private const val MAX_ATTRIBUTION_LENGTH = 500
private const val DESCRIPTION = "Template descriptions"
private const val ATTRIBUTION = "Template attributions"

/** Copies of a published template need to know the terms they were made under. */
private fun requireLicenseIfPublished(published: Boolean, license: License?) {
  if (published && license == null) {
    throw InvalidInputException("A published template needs a licence")
  }
}

@Service
class TemplateServiceImpl(
    private val templateRepository: TemplateRepository,
    private val licenseService: LicenseService,
    private val templateTagService: TemplateTagServiceImpl,
    private val templateQuestionnaireService: TemplateQuestionnaireService,
    private val permissionEvaluator: PermissionEvaluator,
) : TemplateRepository by templateRepository {

  @Transactional
  fun createFromQuestionnaire(input: CreateTemplateFromQuestionnaireInput): Template {
    // A template's own questionnaire is room-less and cannot be the source of another template.
    if (!templateQuestionnaireService.isRoomQuestionnaire(input.questionnaireId)) {
      throw InvalidInputException("Only questionnaires of a room can become templates")
    }
    val name = validateName(input.name)
    val license = resolveLicense(input.licenseId, findInheritedLicenseId(input.questionnaireId))
    requireLicenseIfPublished(input.published, license)
    val id = templateQuestionnaireService.createRoomlessCopy(input.questionnaireId, name)
    val template =
        Template(
            id = id,
            name = name,
            description =
                input.description?.let {
                  validateOptionalText(it, MAX_DESCRIPTION_LENGTH, DESCRIPTION)
                },
            language = input.language,
            license = license,
            attribution =
                input.attribution?.let {
                  validateOptionalText(it, MAX_ATTRIBUTION_LENGTH, ATTRIBUTION)
                },
            aiGenerated = input.aiGenerated,
            published = input.published,
            sourceQuestionnaireId = input.questionnaireId,
            tags = templateTagService.findOrCreateByNames(input.language, input.tagNames.orEmpty()))
    return templateRepository.save(template)
  }

  /** Applies the non-null fields of [input]; a blank string clears description or attribution. */
  @Transactional
  fun update(input: UpdateTemplateInput): Template {
    val template = findTemplate(input.id)
    input.name?.let {
      val name = validateName(it)
      templateQuestionnaireService.renameRoomless(input.id, name)
      template.name = name
    }
    input.description?.let {
      template.description = validateOptionalText(it, MAX_DESCRIPTION_LENGTH, DESCRIPTION)
    }
    input.attribution?.let {
      template.attribution = validateOptionalText(it, MAX_ATTRIBUTION_LENGTH, ATTRIBUTION)
    }
    input.licenseId?.let { updateLicense(template, it) }
    input.aiGenerated?.let { template.aiGenerated = it }
    input.published?.let { template.published = it }
    updateLanguageAndTags(template, input.language, input.tagNames)
    requireLicenseIfPublished(template.published, template.license)
    return templateRepository.save(template)
  }

  /**
   * Tags belong to a language, so a new language without new [tagNames] clears the tags rather than
   * creating unverified tags with names of the old language.
   */
  private fun updateLanguageAndTags(
      template: Template,
      language: String?,
      tagNames: List<String>?
  ) {
    val languageChanged = language != null && language != template.language
    language?.let { template.language = it }
    val names = tagNames ?: emptyList<String>().takeIf { languageChanged }
    names?.let { template.tags = templateTagService.findOrCreateByNames(template.language!!, it) }
  }

  /**
   * A licence cannot be changed once set, since copies may already have been made under it. This is
   * also what keeps the licence a template inherited from its source, see [resolveLicense].
   */
  private fun updateLicense(template: Template, licenseId: String) {
    val current = template.license
    if (current?.id == licenseId) return
    if (current != null) throw InvalidInputException("The licence of a template cannot be changed")
    template.license = findSelectableLicense(licenseId)
  }

  /**
   * Replaces the template's contents and questionnaire settings with the current state of the
   * questionnaire it was created from, which the caller must still be allowed to edit.
   */
  @Transactional
  fun updateFromSource(id: UUID): Template {
    val template = findTemplate(id)
    val sourceId = template.sourceQuestionnaireId ?: throw TemplateSourceNotFoundException(id)
    if (!templateQuestionnaireService.isRoomQuestionnaire(sourceId)) {
      throw TemplateSourceNotFoundException(id)
    }
    if (!mayEditQuestionnaire(sourceId)) {
      throw AccessDeniedException("Editing the source questionnaire is not permitted")
    }
    findInheritedLicenseId(sourceId)?.let { resolveLicense(template.license?.id, it) }
    templateQuestionnaireService.replaceRoomlessContents(id, sourceId)
    // Marks the template as changed for the list of the creator's templates.
    template.auditMetadata.updatedAt = Instant.now()
    return templateRepository.save(template)
  }

  @Transactional
  fun delete(id: UUID): UUID {
    val template = findTemplate(id)
    templateRepository.delete(template)
    templateRepository.flush()
    templateQuestionnaireService.deleteRoomless(id)
    return id
  }

  @Transactional
  fun deleteUnpublishedByCreator(userId: UUID) {
    val ids =
        templateRepository.findByAuditMetadataCreatedByAndPublishedFalse(userId).mapNotNull {
          it.id
        }
    ids.forEach { delete(it) }
  }

  /**
   * The licence of the templates the questionnaire's contents were copied from. A copy of a
   * licensed template may only be republished under exactly that licence, so contents copied under
   * different licences cannot be combined into a template at all.
   */
  private fun findInheritedLicenseId(questionnaireId: UUID): String? {
    val licenseIds =
        templateQuestionnaireService
            .findContentsCopiedFromTemplates(questionnaireId)
            .mapNotNull { it.sourceTemplateLicenseId }
            .distinct()
    if (licenseIds.size > 1) {
      throw InvalidInputException(
          "Contents copied under different licences cannot form a template: $licenseIds")
    }
    return licenseIds.firstOrNull()
  }

  /** An inherited licence is kept even once retired, since the copies were made under it. */
  private fun resolveLicense(requestedId: String?, inheritedId: String?): License? {
    if (inheritedId == null) return requestedId?.let(::findSelectableLicense)
    if (requestedId != inheritedId) {
      throw InvalidInputException(
          "Copies of a template licensed under $inheritedId must keep that licence")
    }
    return licenseService.findById(inheritedId)
        ?: throw InvalidInputException("Unknown licence: $inheritedId")
  }

  fun findSelectableLicenses(): List<License> = licenseService.findSelectable()

  private fun mayEditQuestionnaire(questionnaireId: UUID): Boolean {
    val authentication = SecurityContextHolder.getContext().authentication ?: return false
    return permissionEvaluator.hasPermission(
        authentication, questionnaireId, "Questionnaire", "write")
  }

  private fun findTemplate(id: UUID): Template =
      templateRepository.findByIdOrNull(id) ?: throw TemplateNotFoundException(id)

  private fun findSelectableLicense(id: String): License =
      licenseService.findById(id)?.takeIf { it.selectable }
          ?: throw InvalidInputException("Unknown or retired licence: $id")

  private fun validateName(name: String): String {
    val trimmed = name.trim()
    if (trimmed.isEmpty() || trimmed.length > MAX_NAME_LENGTH) {
      throw InvalidInputException("Template names must have 1 to $MAX_NAME_LENGTH characters")
    }
    return trimmed
  }

  /** Trims [text] and reads a blank one as absent. */
  private fun validateOptionalText(text: String, maxLength: Int, subject: String): String? {
    val trimmed = text.trim()
    if (trimmed.length > maxLength) {
      throw InvalidInputException("$subject must not exceed $maxLength characters")
    }
    return trimmed.ifEmpty { null }
  }

  @Deprecated(
      "Deprecated by base implementation", replaceWith = ReplaceWith("deleteAllInBatch(entities)"))
  override fun deleteInBatch(entities: Iterable<Template>) =
      templateRepository.deleteInBatch(entities)
}
