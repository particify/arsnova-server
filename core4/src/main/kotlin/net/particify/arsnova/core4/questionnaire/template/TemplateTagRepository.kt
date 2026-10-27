/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.template

import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface TemplateTagRepository : JpaRepository<TemplateTag, UUID> {
  fun findByLanguageOrderByName(language: String): List<TemplateTag>

  fun findByLanguageAndVerifiedTrueOrderByName(language: String): List<TemplateTag>

  fun findByLanguageAndNameIgnoreCase(language: String, name: String): TemplateTag?

  /** Pairs of a template id and one of its tags. */
  @Query(
      "SELECT t.id, tag FROM Template t JOIN t.tags tag WHERE t.id IN :templateIds " +
          "ORDER BY lower(tag.name)")
  fun findWithTemplateIdByTemplateIds(templateIds: Collection<UUID>): List<Array<Any>>

  /** Leaves an existing tag of the same name, in any case, alone instead of failing. */
  @Modifying
  @Query(
      value =
          "INSERT INTO questionnaire.template_tag (id, language, name, verified) " +
              "VALUES (:id, :language, :name, false) " +
              "ON CONFLICT (language, lower(name)) DO NOTHING",
      nativeQuery = true)
  fun insertIfAbsent(id: UUID, language: String, name: String): Int

  @Modifying
  @Query(
      value = "DELETE FROM questionnaire.template_tag_mapping WHERE tag_id = :tagId",
      nativeQuery = true)
  fun removeTagFromTemplatesByTagId(tagId: UUID): Int
}
