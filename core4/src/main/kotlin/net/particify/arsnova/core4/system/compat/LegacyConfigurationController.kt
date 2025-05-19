/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.compat

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class LegacyConfigurationController {
  @GetMapping("/configuration")
  fun configuration(): LegacyConfiguration {
    return LegacyConfiguration()
  }

  data class LegacyConfiguration(
      val authenticationProviders: List<Any> =
          listOf(
              mapOf(
                  "id" to "user-db",
                  "title" to "arsnova",
                  "order" to 0,
                  "allowedRoles" to arrayOf("MODERATOR", "PARTICIPANT"),
                  "type" to "USERNAME_PASSWORD")),
      val features: Map<String, Any> = mapOf(),
      val ui: Map<String, Any> = mapOf()
  )
}
