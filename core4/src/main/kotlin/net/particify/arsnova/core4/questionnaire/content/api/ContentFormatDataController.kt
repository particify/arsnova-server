/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.content.api

import net.particify.arsnova.core4.questionnaire.content.Content
import net.particify.arsnova.core4.questionnaire.content.ContentFormatData
import net.particify.arsnova.core4.questionnaire.content.SolutionVisibility
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

/**
 * Every path that serves format data, subscriptions included, resolves it here. Field resolvers run
 * in the security context of the request or subscription they belong to, so the solution is
 * withheld per reader.
 */
@Controller
@PreAuthorize("hasRole('USER')")
class ContentFormatDataController(private val solutionVisibility: SolutionVisibility) {
  @SchemaMapping(typeName = "Content")
  fun formatData(content: Content): ContentFormatData {
    val formatData = checkNotNull(content.formatData)
    return if (solutionVisibility.maySeeSolution(content)) formatData
    else formatData.withoutSolution()
  }
}
