/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.config

import jakarta.validation.Valid
import java.net.URL
import net.particify.arsnova.core4.common.LanguageIso639
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
    val verificationUriPattern: String,
    val passwordResetUriPattern: String,
    @field:Valid val footer: List<Footer> = emptyList()
) {
  data class Footer(
      @field:LanguageIso639 val language: String? = null,
      val lines: List<List<String>> = emptyList(),
      val links: List<FooterLink> = emptyList(),
      val logo: FooterLogo? = null
  ) {
    data class FooterLink(val text: String, val url: URL)

    data class FooterLogo(
        val imageUrl: URL,
        val linkUrl: URL,
        val altText: String,
        val width: Int? = null,
        val height: Int? = null
    )
  }
}
