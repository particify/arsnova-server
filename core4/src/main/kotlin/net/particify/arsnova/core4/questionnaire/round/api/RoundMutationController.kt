/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.round.api

import java.util.UUID
import net.particify.arsnova.core4.questionnaire.round.Round
import net.particify.arsnova.core4.questionnaire.round.RoundServiceImpl
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

@Controller
@PreAuthorize("hasRole('USER')")
@SchemaMapping(typeName = "Mutation")
class RoundMutationController(private val roundServiceImpl: RoundServiceImpl) {
  @MutationMapping
  @PreAuthorize("hasPermission(#contentId, 'Content', 'write')")
  fun startQuestionnaireContentRound(@Argument contentId: UUID): Round {
    return roundServiceImpl.startNewRound(contentId)
  }
}
