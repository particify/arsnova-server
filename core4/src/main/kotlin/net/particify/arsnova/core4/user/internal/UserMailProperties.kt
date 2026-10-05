/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user.internal

import org.springframework.boot.context.properties.ConfigurationProperties

/** Shares the `mail` prefix with the mail module's properties to keep the existing keys. */
@ConfigurationProperties(prefix = "mail")
data class UserMailProperties(
    val invitationUriPattern: String,
    val verificationUriPattern: String,
    val passwordResetUriPattern: String
)
