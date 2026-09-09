/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user.internal

/** Which asserted value becomes the username of an account authenticated through SAML. */
enum class UsernameMapping {
  ID,
  MAIL_ADDRESS,
}

/**
 * Determines the username for a SAML user, or `null` if the mapped value is not available. The
 * value is lowercased because usernames are looked up lowercased, both as a display ID and on
 * login.
 */
internal fun resolveUsername(
    mapping: UsernameMapping,
    externalId: String,
    mailAddress: String?
): String? =
    when (mapping) {
      UsernameMapping.ID -> externalId.lowercase()
      UsernameMapping.MAIL_ADDRESS -> mailAddress?.lowercase()
    }
