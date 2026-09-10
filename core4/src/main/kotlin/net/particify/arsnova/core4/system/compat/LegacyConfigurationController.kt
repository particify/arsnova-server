/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.compat

import net.particify.arsnova.core4.system.compat.LegacyConfigurationController.LegacyConfiguration.LegacyAuthenticationProvider
import net.particify.arsnova.core4.system.config.ServiceProperties
import net.particify.arsnova.core4.system.config.UiProperties
import net.particify.arsnova.core4.system.security.RoomCreationPolicy
import net.particify.arsnova.core4.user.internal.ExtendedSaml2RelyingPartyProperties
import net.particify.arsnova.core4.user.internal.LdapProperties
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class LegacyConfigurationController(
    private val ldapProperties: LdapProperties,
    private val roomCreationPolicy: RoomCreationPolicy,
    private val saml2Properties: ExtendedSaml2RelyingPartyProperties,
    private val serviceProperties: ServiceProperties,
    private val uiProperties: UiProperties
) {
  @GetMapping("/configuration")
  fun configuration(): LegacyConfiguration {
    return LegacyConfiguration(
        buildSaml2ProviderList()
            .plus(
                LegacyAuthenticationProvider(
                    id = "user-db",
                    title = serviceProperties.productName,
                    order = 0,
                    allowedRoles =
                        allowedRoles(roomCreationPolicy.mayVerifiedAccountsCreateRooms()),
                    type = LegacyAuthenticationProvider.Type.USERNAME_PASSWORD))
            .plus(buildLdapProviderList())
            .plus(
                LegacyAuthenticationProvider(
                    id = "guest",
                    title = "guest",
                    order = 0,
                    allowedRoles =
                        allowedRoles(roomCreationPolicy.mayUnverifiedAccountsCreateRooms()),
                    type = LegacyAuthenticationProvider.Type.ANONYMOUS)),
        mapOf(),
        uiProperties.ui)
  }

  private fun buildLdapProviderList(): List<LegacyAuthenticationProvider> {
    return ldapProperties.registration.map {
      LegacyAuthenticationProvider(
          id = it.key.toString(),
          title = it.value.title,
          order = it.value.order,
          allowedRoles = allowedRoles(roomCreationPolicy.mayVerifiedAccountsCreateRooms()),
          type = LegacyAuthenticationProvider.Type.USERNAME_PASSWORD,
      )
    }
  }

  private fun buildSaml2ProviderList(): List<LegacyAuthenticationProvider> {
    return saml2Properties.registration.map {
      LegacyAuthenticationProvider(
          id = it.key.toString(),
          title = it.value.title,
          order = it.value.order,
          allowedRoles = allowedRoles(roomCreationPolicy.mayVerifiedAccountsCreateRooms()),
          type = LegacyAuthenticationProvider.Type.SSO,
      )
    }
  }

  /** The legacy moderator role is what clients read as permission to create rooms. */
  private fun allowedRoles(mayCreateRooms: Boolean): List<LegacyAuthenticationProvider.Role> {
    return if (mayCreateRooms)
        listOf(
            LegacyAuthenticationProvider.Role.MODERATOR,
            LegacyAuthenticationProvider.Role.PARTICIPANT)
    else listOf(LegacyAuthenticationProvider.Role.PARTICIPANT)
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
