/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.content

import org.springframework.security.access.PermissionEvaluator
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

/**
 * Decides whether the current user may see a content's solution: everyone once the questionnaire
 * publishes it, and before that whoever may edit the content or moderate its room, as in v3.
 */
@Component
class SolutionVisibility(private val permissionEvaluator: PermissionEvaluator) {
  fun maySeeSolution(content: Content): Boolean {
    val questionnaire = content.questionnaire ?: return false
    if (questionnaire.correctOptionsPublished == true) return true
    val authentication = SecurityContextHolder.getContext().authentication ?: return false
    val contentId = content.id ?: return false
    if (permissionEvaluator.hasPermission(authentication, contentId, "Content", "write")) {
      return true
    }
    val roomId = questionnaire.roomId ?: return false
    return permissionEvaluator.hasPermission(authentication, roomId, "Room", "moderate")
  }
}
