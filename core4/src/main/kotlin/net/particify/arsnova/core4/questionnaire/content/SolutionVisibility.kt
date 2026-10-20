/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.content

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
    return questionnaire.correctOptionsPublished == true ||
        isPrivileged(permissionEvaluator, content)
  }

  /** Statistics are the answer counts and the round stats derived from the answers. */
  fun maySeeStatistics(content: Content): Boolean {
    val questionnaire = content.questionnaire ?: return false
    return questionnaire.statisticsPublished == true || isPrivileged(permissionEvaluator, content)
  }

  companion object {
    /** Whether the current user may edit [content] or moderate its room. */
    fun isPrivileged(permissionEvaluator: PermissionEvaluator, content: Content): Boolean {
      val authentication = SecurityContextHolder.getContext().authentication ?: return false
      val contentId = content.id ?: return false
      if (permissionEvaluator.hasPermission(authentication, contentId, "Content", "write")) {
        return true
      }
      val roomId = content.questionnaire?.roomId ?: return false
      return permissionEvaluator.hasPermission(authentication, roomId, "Room", "moderate")
    }
  }
}
