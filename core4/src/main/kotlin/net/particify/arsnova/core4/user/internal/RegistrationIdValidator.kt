/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user.internal

import net.particify.arsnova.core4.user.sso.ExtendedSaml2RelyingPartyProperties
import net.particify.arsnova.core4.user.sso.LdapProperties
import net.particify.arsnova.core4.user.sso.OidcProperties
import org.springframework.stereotype.Component

/**
 * Rejects a registration ID which is configured for more than one login protocol while the
 * application starts. External logins are keyed by registration ID and external ID alone, so two
 * registrations sharing an ID would share their accounts: a user of one whose ID equals that of a
 * user of the other would be logged in to that user's account.
 */
@Component
class RegistrationIdValidator(
    ldapProperties: LdapProperties,
    saml2Properties: ExtendedSaml2RelyingPartyProperties,
    oidcProperties: OidcProperties
) {
  init {
    val protocolsById =
        mapOf(
                "LDAP" to ldapProperties.registration.keys,
                "OIDC" to oidcProperties.registration.keys,
                "SAML" to saml2Properties.registration.keys)
            .flatMap { (protocol, ids) -> ids.map { it to protocol } }
            .groupBy({ it.first }, { it.second })
            .filterValues { it.size > 1 }
    require(protocolsById.isEmpty()) {
      protocolsById.entries.joinToString(" ") { (id, protocols) ->
        "Registration ID $id is configured for more than one protocol: " +
            "${protocols.joinToString(", ")}."
      } + " Every registration needs an ID of its own."
    }
  }
}
