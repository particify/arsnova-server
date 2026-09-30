/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.template

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID
import net.particify.arsnova.core4.common.UuidGenerator

/** Tags are shared by all templates of a [language] and only listed publicly once verified. */
@Entity
@Table(schema = "questionnaire", name = "template_tag")
class TemplateTag(
    @Id @UuidGenerator var id: UUID? = null,
    var language: String? = null,
    var name: String? = null,
    var verified: Boolean = false,
)
