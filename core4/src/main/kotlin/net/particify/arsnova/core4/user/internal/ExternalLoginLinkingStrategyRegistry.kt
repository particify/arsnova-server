/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user.internal

import java.util.UUID
import net.particify.arsnova.core4.user.ExternalLoginLinkingStrategy
import org.springframework.stereotype.Component

/**
 * Resolves the linking strategy which a login provider registration selects by name.
 *
 * A name carried by none of the registered strategies is rejected while the application starts
 * rather than when a login first reaches that registration: a login which fails over a
 * configuration mistake locks out everyone the registration serves and says nothing about why.
 */
@Component
class ExternalLoginLinkingStrategyRegistry(
    strategies: List<ExternalLoginLinkingStrategy>,
    ldapProperties: LdapProperties,
    saml2Properties: ExtendedSaml2RelyingPartyProperties
) {
  private val strategiesByName = strategies.associateBy { it.name }
  private val availableNames = strategiesByName.keys.sorted().joinToString(", ").ifEmpty { "none" }

  init {
    require(strategiesByName.size == strategies.size) {
      "Linking strategies do not carry distinct names: ${strategies.map { it.name }.sorted()}."
    }
    validate(ldapProperties.registration.mapValues { it.value.linkingStrategy })
    validate(saml2Properties.registration.mapValues { it.value.linkingStrategy })
  }

  /** `null` for a registration which selects no strategy, which is then never linked. */
  fun find(name: String?): ExternalLoginLinkingStrategy? = name?.let { strategiesByName[it] }

  private fun validate(selectedByRegistration: Map<UUID, String?>) {
    for ((registrationId, name) in selectedByRegistration) {
      require(name == null || strategiesByName.containsKey(name)) {
        "Registration $registrationId selects the unknown linking strategy \"$name\". " +
            "Available strategies: $availableNames."
      }
    }
  }
}
