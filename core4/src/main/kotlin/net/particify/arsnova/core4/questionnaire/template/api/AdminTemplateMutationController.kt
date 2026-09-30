/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.template.api

import java.util.UUID
import net.particify.arsnova.core4.questionnaire.template.TemplateServiceImpl
import net.particify.arsnova.core4.questionnaire.template.TemplateTag
import net.particify.arsnova.core4.questionnaire.template.TemplateTagServiceImpl
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

@Controller
@PreAuthorize("hasRole('ADMIN')")
@SchemaMapping(typeName = "Mutation")
class AdminTemplateMutationController(
    private val templateService: TemplateServiceImpl,
    private val templateTagService: TemplateTagServiceImpl,
) {
  @MutationMapping
  fun adminDeleteTemplate(@Argument id: UUID): UUID {
    return templateService.delete(id)
  }

  @MutationMapping
  fun adminDeleteTemplateTag(@Argument id: UUID): UUID {
    return templateTagService.delete(id)
  }

  @MutationMapping
  fun adminUpdateTemplateTagName(@Argument id: UUID, @Argument name: String): TemplateTag {
    return templateTagService.updateName(id, name)
  }

  @MutationMapping
  fun adminUpdateTemplateTagVerified(@Argument id: UUID, @Argument verified: Boolean): TemplateTag {
    return templateTagService.updateVerified(id, verified)
  }
}
