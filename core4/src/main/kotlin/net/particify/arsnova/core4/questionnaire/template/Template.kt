/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.template

import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.util.UUID
import net.particify.arsnova.core4.common.AuditMetadata
import net.particify.arsnova.core4.common.LanguageIso639
import net.particify.arsnova.core4.license.License

/**
 * A reusable question series. Its contents live in a room-less questionnaire which shares the
 * template's [id], so the questionnaire content operations work on templates unchanged.
 */
@Entity
@Table(schema = "questionnaire")
class Template(
    @Id var id: UUID? = null,
    // Null until persisted: the id is assigned, so the version is what marks a template as new.
    @Version var version: Int? = null,
    var name: String? = null,
    var description: String? = null,
    @field:LanguageIso639 var language: String? = null,
    @ManyToOne var license: License? = null,
    var attribution: String? = null,
    var aiGenerated: Boolean = false,
    var published: Boolean = false,
    var sourceQuestionnaireId: UUID? = null,
    // Lazy: the GraphQL tags field loads them for a whole page at once.
    @ManyToMany
    @JoinTable(
        name = "template_tag_mapping",
        schema = "questionnaire",
        joinColumns = [JoinColumn(name = "template_id")],
        inverseJoinColumns = [JoinColumn(name = "tag_id")])
    var tags: MutableSet<TemplateTag> = mutableSetOf(),
    @Embedded var auditMetadata: AuditMetadata = AuditMetadata(),
)
