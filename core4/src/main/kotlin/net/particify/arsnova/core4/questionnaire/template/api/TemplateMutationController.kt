/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.template.api

import java.util.UUID
import net.particify.arsnova.core4.questionnaire.exception.TemplateNotFoundException
import net.particify.arsnova.core4.questionnaire.questionnaire.Questionnaire
import net.particify.arsnova.core4.questionnaire.template.Template
import net.particify.arsnova.core4.questionnaire.template.TemplateQuestionnaireService
import net.particify.arsnova.core4.questionnaire.template.TemplateServiceImpl
import org.springframework.data.repository.findByIdOrNull
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

@Controller
@PreAuthorize("hasRole('USER')")
@SchemaMapping(typeName = "Mutation")
class TemplateMutationController(
    private val templateService: TemplateServiceImpl,
    private val templateQuestionnaireService: TemplateQuestionnaireService,
) {
  @MutationMapping
  @PreAuthorize("hasPermission(#input.questionnaireId, 'Questionnaire', 'write')")
  fun createTemplateFromQuestionnaire(
      @Argument input: CreateTemplateFromQuestionnaireInput
  ): Template {
    return templateService.createFromQuestionnaire(input)
  }

  @MutationMapping
  @PreAuthorize("hasPermission(#input.id, 'Template', 'write')")
  fun updateTemplate(@Argument input: UpdateTemplateInput): Template {
    return templateService.update(input)
  }

  @MutationMapping
  @PreAuthorize("hasPermission(#id, 'Template', 'write')")
  fun updateTemplateFromSource(@Argument id: UUID): Template {
    return templateService.updateFromSource(id)
  }

  @MutationMapping
  @PreAuthorize("hasPermission(#id, 'Template', 'delete')")
  fun deleteTemplate(@Argument id: UUID): UUID {
    return templateService.delete(id)
  }

  @MutationMapping
  @PreAuthorize(
      "hasPermission(#input.templateId, 'Template', 'read') and " +
          "hasPermission(#input.roomId, 'Room', 'write')")
  fun createQuestionnaireFromTemplate(
      @Argument input: CreateQuestionnaireFromTemplateInput
  ): Questionnaire {
    val template =
        templateService.findByIdOrNull(input.templateId)
            ?: throw TemplateNotFoundException(input.templateId)
    return templateQuestionnaireService.createRoomCopy(
        template, input.roomId, input.name ?: template.name!!)
  }
}
