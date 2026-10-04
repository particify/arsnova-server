/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.migration.v3

import net.particify.arsnova.core4.user.internal.LdapProperties
import org.springframework.stereotype.Component

/** Fails startup if the LDAP registrations cannot be mapped unambiguously by the migration. */
@Component
class LdapRegistrationValidator(
    migrationProperties: MigrationProperties,
    ldapProperties: LdapProperties
) {
  init {
    require(!migrationProperties.enabled || ldapProperties.registration.size <= 1) {
      "Only a single LDAP registration is supported while the v3 migration is enabled because " +
          "persistence.v3-migration.authentication-provider-mapping maps the v3 provider name " +
          "\"LDAP\" to exactly one provider ID. Migrated users would end up in whichever " +
          "registration matches that mapping. Configured registrations: " +
          "${ldapProperties.registration.keys}."
    }
  }
}
