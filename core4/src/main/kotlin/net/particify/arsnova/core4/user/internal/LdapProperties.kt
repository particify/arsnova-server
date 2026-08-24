/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user.internal

import java.time.Duration
import java.util.UUID
import org.springframework.boot.context.properties.ConfigurationProperties

private const val DEFAULT_TITLE = "LDAP"
private const val DEFAULT_USER_ID_ATTRIBUTE = "uid"
private const val DEFAULT_CONNECT_TIMEOUT_SECONDS = 5L
private const val DEFAULT_READ_TIMEOUT_SECONDS = 10L

/**
 * An empty registration map disables LDAP authentication, so no defaults are shipped in
 * `application.yaml`.
 */
@ConfigurationProperties("security.ldap")
data class LdapProperties(val registration: Map<UUID, Registration> = mapOf()) {
  @Suppress("LongParameterList")
  data class Registration(
      /**
       * URL of the directory, including the base DN, e.g.
       * `ldap://ldap.example.com/dc=example,dc=com`.
       */
      val url: String,
      /**
       * DN pattern relative to the base DN, e.g. `uid={0},ou=people`. Excludes [userSearchFilter].
       */
      val userDnPattern: String? = null,
      val userSearchBase: String = "",
      /** Search filter, e.g. `(uid={0})`. Excludes [userDnPattern]. */
      val userSearchFilter: String? = null,
      val managerUserDn: String? = null,
      val managerPassword: String? = null,
      /** Attribute holding the canonical user ID, `sAMAccountName` for Active Directory. */
      val userIdAttribute: String = DEFAULT_USER_ID_ATTRIBUTE,
      /** Standard attribute names imported into the user's profile, see [ImportedAttribute]. */
      val importedAttributes: List<String> = listOf(),
      val connectTimeout: Duration = Duration.ofSeconds(DEFAULT_CONNECT_TIMEOUT_SECONDS),
      val readTimeout: Duration = Duration.ofSeconds(DEFAULT_READ_TIMEOUT_SECONDS),
      /** Server-side limit for searches. Unlimited if unset. Not applicable to the bind itself. */
      val searchTimeLimit: Duration? = null,
      val title: String = DEFAULT_TITLE,
      val order: Int = 0
  ) {
    /** Attributes requested from the directory: those consumed, and nothing else. */
    val requestedAttributes: List<String> =
        listOf(userIdAttribute).plus(importedAttributes).distinct()

    init {
      require(userDnPattern.isNullOrEmpty() != userSearchFilter.isNullOrEmpty()) {
        "Exactly one of user-dn-pattern and user-search-filter needs to be set for an LDAP " +
            "registration. It determines whether users are located by DN pattern or by search."
      }
      val unsupported = importedAttributes.filter { ImportedAttribute.byAttributeName(it) == null }
      require(unsupported.isEmpty()) {
        "Unsupported LDAP attributes in imported-attributes: $unsupported. Supported attributes: " +
            "${ImportedAttribute.attributeNames()}."
      }
    }
  }

  /** Directory attributes which can be imported into a user's profile, by standard name. */
  enum class ImportedAttribute(val attributeName: String) {
    GIVEN_NAME("givenName"),
    MAIL("mail"),
    SURNAME("sn");

    companion object {
      fun byAttributeName(attributeName: String): ImportedAttribute? =
          entries.find { it.attributeName == attributeName }

      fun attributeNames(): List<String> = entries.map { it.attributeName }
    }
  }
}
