/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@ConfigurationProperties(prefix = "mail")
@Validated
data class MailProperties(
    val senderAddress: String,
    val senderName: String,
    val host: String? = null,
    val port: Int = 0,
    val implicitTls: Boolean = false,
    val username: String? = null,
    val password: String? = null,
    val localhost: String? = null,
    val invitationUriPattern: String,
    val verificationUriPattern: String
)
