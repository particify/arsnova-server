/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.compat

import net.particify.arsnova.core4.system.compat.LegacyConfigurationController.LegacyConfiguration.LegacyAuthenticationProvider
import net.particify.arsnova.core4.user.internal.ExtendedSaml2RelyingPartyProperties
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class LegacyConfigurationController(
    private val saml2Properties: ExtendedSaml2RelyingPartyProperties
) {
  @GetMapping("/configuration")
  fun configuration(): LegacyConfiguration {
    return LegacyConfiguration(
        buildSaml2ProviderList()
            .plus(
                LegacyAuthenticationProvider(
                    id = "user-db",
                    title = "arsnova",
                    order = 0,
                    allowedRoles =
                        listOf(
                            LegacyAuthenticationProvider.Role.MODERATOR,
                            LegacyAuthenticationProvider.Role.PARTICIPANT),
                    type = LegacyAuthenticationProvider.Type.USERNAME_PASSWORD))
            .plus(
                LegacyAuthenticationProvider(
                    id = "guest",
                    title = "guest",
                    order = 0,
                    allowedRoles =
                        listOf(
                            LegacyAuthenticationProvider.Role.MODERATOR,
                            LegacyAuthenticationProvider.Role.PARTICIPANT),
                    type = LegacyAuthenticationProvider.Type.ANONYMOUS)))
  }

  private fun buildSaml2ProviderList(): List<LegacyAuthenticationProvider> {
    return saml2Properties.registration.map {
      LegacyAuthenticationProvider(
          id = it.key.toString(),
          title = "SAML",
          order = 0,
          allowedRoles =
              listOf(
                  LegacyAuthenticationProvider.Role.MODERATOR,
                  LegacyAuthenticationProvider.Role.PARTICIPANT),
          type = LegacyAuthenticationProvider.Type.SSO,
      )
    }
  }

  data class LegacyConfiguration(
      val authenticationProviders: List<LegacyAuthenticationProvider>,
      val features: Map<String, Any> = mapOf(),
      val ui: Map<String, Any> = mapOf()
  ) {
    data class LegacyAuthenticationProvider(
        val id: String,
        val title: String,
        val order: Int,
        val allowedRoles: List<Role>,
        val type: Type
    ) {
      enum class Role {
        MODERATOR,
        PARTICIPANT
      }

      enum class Type {
        USERNAME_PASSWORD,
        SSO,
        ANONYMOUS
      }
    }
  }
}
