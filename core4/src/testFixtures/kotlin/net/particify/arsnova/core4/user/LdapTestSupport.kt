/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user

import org.springframework.test.context.DynamicPropertyRegistry

/**
 * Points one directory registration at a server. The URL carries the base DN, against which
 * [userDnPattern] is resolved.
 */
fun registerLdapDirectory(
    registry: DynamicPropertyRegistry,
    registrationId: String,
    url: String,
    userDnPattern: String,
    importedAttributes: List<String>
) {
  val prefix = "security.ldap.registration.$registrationId"
  registry.add("$prefix.url") { url }
  registry.add("$prefix.user-dn-pattern") { userDnPattern }
  registry.add("$prefix.imported-attributes") { importedAttributes.joinToString(",") }
}
