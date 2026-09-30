/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.template

import jakarta.transaction.Transactional
import java.util.UUID
import net.particify.arsnova.core4.common.exception.InvalidInputException
import net.particify.arsnova.core4.questionnaire.exception.TemplateTagNotFoundException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

private const val MAX_TAG_NAME_LENGTH = 50
private const val MAX_TAGS_PER_TEMPLATE = 20

@Service
class TemplateTagServiceImpl(private val templateTagRepository: TemplateTagRepository) {

  fun findByLanguage(language: String, verifiedOnly: Boolean): List<TemplateTag> =
      if (verifiedOnly) templateTagRepository.findByLanguageAndVerifiedTrueOrderByName(language)
      else templateTagRepository.findByLanguageOrderByName(language)

  /** The tags of each template, sorted by name. Templates without tags are left out. */
  fun findByTemplateIds(templateIds: Collection<UUID>): Map<UUID, List<TemplateTag>> {
    if (templateIds.isEmpty()) return emptyMap()
    return templateTagRepository
        .findWithTemplateIdByTemplateIds(templateIds)
        .groupBy({ it[0] as UUID }, { it[1] as TemplateTag })
  }

  /**
   * Resolves tag names case-insensitively within [language], creating unverified tags for names not
   * known yet. A tag created concurrently by another request is used instead of failing.
   */
  @Transactional
  fun findOrCreateByNames(language: String, names: List<String>): MutableSet<TemplateTag> {
    val distinctNames = names.map { normalizeName(it) }.distinctBy { it.lowercase() }
    if (distinctNames.size > MAX_TAGS_PER_TEMPLATE) {
      throw InvalidInputException("A template can have at most $MAX_TAGS_PER_TEMPLATE tags")
    }
    return distinctNames.map { findOrCreate(language, it) }.toMutableSet()
  }

  private fun findOrCreate(language: String, name: String): TemplateTag {
    templateTagRepository.findByLanguageAndNameIgnoreCase(language, name)?.let {
      return it
    }
    templateTagRepository.insertIfAbsent(UUID.randomUUID(), language, name)
    return templateTagRepository.findByLanguageAndNameIgnoreCase(language, name)
        ?: error("Tag '$name' is missing after its creation")
  }

  /**
   * Concurrent renames can both pass the duplicate check, so the unique index on the language and
   * the name has the last word and its violation is reported like the check's.
   */
  @Transactional
  fun updateName(id: UUID, name: String): TemplateTag {
    val tag = findTag(id)
    val normalizedName = normalizeName(name)
    val existing =
        templateTagRepository.findByLanguageAndNameIgnoreCase(tag.language!!, normalizedName)
    if (existing != null && existing.id != id) throw duplicateNameException()
    tag.name = normalizedName
    return try {
      templateTagRepository.saveAndFlush(tag)
    } catch (_: DataIntegrityViolationException) {
      throw duplicateNameException()
    }
  }

  private fun duplicateNameException() =
      InvalidInputException("A tag with this name already exists for the language")

  @Transactional
  fun updateVerified(id: UUID, verified: Boolean): TemplateTag {
    val tag = findTag(id)
    tag.verified = verified
    return templateTagRepository.save(tag)
  }

  @Transactional
  fun delete(id: UUID): UUID {
    findTag(id)
    templateTagRepository.removeTagFromTemplatesByTagId(id)
    templateTagRepository.deleteById(id)
    return id
  }

  private fun findTag(id: UUID): TemplateTag =
      templateTagRepository.findByIdOrNull(id) ?: throw TemplateTagNotFoundException(id)

  private fun normalizeName(name: String): String {
    val trimmed = name.trim()
    if (trimmed.isEmpty() || trimmed.length > MAX_TAG_NAME_LENGTH) {
      throw InvalidInputException("Tag names must have 1 to $MAX_TAG_NAME_LENGTH characters")
    }
    return trimmed
  }
}
