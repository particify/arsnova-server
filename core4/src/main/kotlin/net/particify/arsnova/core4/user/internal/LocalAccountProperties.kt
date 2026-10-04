/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user.internal

import org.springframework.boot.context.properties.ConfigurationProperties

private const val DOMAIN_LABEL = "(\\*|[a-z0-9]([a-z0-9-]*[a-z0-9])?)"
private val DOMAIN_PATTERN = Regex("$DOMAIN_LABEL(\\.$DOMAIN_LABEL)*", RegexOption.IGNORE_CASE)

@ConfigurationProperties(prefix = "security.local-account")
data class LocalAccountProperties(
    /**
     * Whether accounts with a username and password exist at all. It gates logging in with one,
     * creating one and every operation on one, so [selfRegistrationEnabled] is without effect while
     * it is `false`.
     */
    val enabled: Boolean,
    /**
     * Whether an account can be created by whoever wants one. Administrators create accounts
     * regardless of this setting.
     */
    val selfRegistrationEnabled: Boolean,
    /**
     * Domains whose addresses may be used for an account, empty for no restriction. A `*` label
     * matches exactly one label of the address' domain, so `*.example.com` covers
     * `mail.example.com` but not `example.com` itself.
     */
    val allowedMailAddressDomains: List<String> = listOf()
) {
  init {
    for (domain in allowedMailAddressDomains) {
      require(DOMAIN_PATTERN.matches(domain)) {
        "\"$domain\" is not a valid mail address domain. Expected dot-separated labels, each " +
            "of them either a wildcard or a domain label."
      }
      require(domain.split(".").any { it != "*" }) {
        "The mail address domain \"$domain\" consists of wildcards only. Omit " +
            "security.local-account.allowed-mail-address-domains to allow any domain."
      }
    }
  }
}
