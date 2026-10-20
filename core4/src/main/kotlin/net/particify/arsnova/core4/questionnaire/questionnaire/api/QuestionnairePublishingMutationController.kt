/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.questionnaire.api

import java.util.UUID
import net.particify.arsnova.core4.questionnaire.questionnaire.Questionnaire
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnairePublishingServiceImpl
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

@Controller
@PreAuthorize("hasRole('USER')")
class QuestionnairePublishingMutationController(
    private val questionnairePublishingService: QuestionnairePublishingServiceImpl
) {

  @MutationMapping
  @PreAuthorize("hasPermission(#contentId, 'Content', 'write')")
  fun startQuestionnaireContent(@Argument contentId: UUID): Questionnaire {
    return questionnairePublishingService.startLiveContent(contentId)
  }

  @MutationMapping
  @PreAuthorize("hasPermission(#questionnaireId, 'Questionnaire', 'write')")
  fun stopQuestionnaireContent(@Argument questionnaireId: UUID): Questionnaire {
    return questionnairePublishingService.stopLiveContent(questionnaireId)
  }

  @MutationMapping
  @PreAuthorize("hasPermission(#questionnaireId, 'Questionnaire', 'write')")
  fun publishQuestionnaireUpToContent(
      @Argument questionnaireId: UUID,
      @Argument contentId: UUID
  ): Questionnaire {
    return questionnairePublishingService.publishUpToContent(questionnaireId, contentId)
  }
}
