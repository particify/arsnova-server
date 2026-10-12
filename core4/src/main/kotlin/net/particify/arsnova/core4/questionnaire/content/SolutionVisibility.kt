/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.content

import net.particify.arsnova.core4.questionnaire.questionnaire.Questionnaire
import org.springframework.security.access.PermissionEvaluator
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

/**
 * Decides whether the current user may see a content's solution and its statistics: everyone once
 * the questionnaire publishes them, and before that whoever may edit the content or moderate its
 * room, as in v3.
 */
@Component
class SolutionVisibility(private val permissionEvaluator: PermissionEvaluator) {
  fun maySeeSolution(content: Content): Boolean {
    val questionnaire = content.questionnaire ?: return false
    return questionnaire.correctOptionsPublished == true || isPrivileged(content, questionnaire)
  }

  /** Statistics are the answer counts and the round stats derived from the answers. */
  fun maySeeStatistics(content: Content): Boolean {
    val questionnaire = content.questionnaire ?: return false
    return questionnaire.statisticsPublished == true || isPrivileged(content, questionnaire)
  }

  private fun isPrivileged(content: Content, questionnaire: Questionnaire): Boolean {
    val authentication = SecurityContextHolder.getContext().authentication ?: return false
    val contentId = content.id ?: return false
    if (permissionEvaluator.hasPermission(authentication, contentId, "Content", "write")) {
      return true
    }
    val roomId = questionnaire.roomId ?: return false
    return permissionEvaluator.hasPermission(authentication, roomId, "Room", "moderate")
  }
}
