/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user.internal

import java.util.UUID

/**
 * What an identity provider asserted about a user at login, already mapped onto account fields.
 * [attributes] are all it released, unmapped, and are handed to the login hooks as they are.
 */
data class ExternalIdentity(
    val registrationId: UUID,
    val externalId: String,
    val mailAddress: String?,
    val givenName: String?,
    val surname: String?,
    val attributes: Map<String, List<Any>>
)
