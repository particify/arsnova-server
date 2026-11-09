/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user.sso

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import java.net.URI
import java.net.URISyntaxException
import java.util.UUID
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

private const val DEFAULT_TITLE = "OIDC"
private const val OPENID_SCOPE = "openid"
private val DEFAULT_SCOPE = listOf(OPENID_SCOPE, "email", "profile")
private val ISSUER_SCHEMES = setOf("http", "https")

/** A scope token as RFC 6749 defines it: printable ASCII except space, `"` and `\`. */
private val SCOPE_TOKEN = Regex("[\\x21\\x23-\\x5B\\x5D-\\x7E]+")

/**
 * OpenID Connect relying party registrations. An empty registration map disables OpenID Connect
 * login, so no defaults are shipped in `application.yaml`.
 */
@ConfigurationProperties("security.oidc")
@Validated
data class OidcProperties(@field:Valid val registration: Map<UUID, Registration> = mapOf()) {
  @Suppress("LongParameterList")
  data class Registration(
      /**
       * Issuer identifier of the OpenID provider. Its discovery document is read from
       * `{issuer-uri}/.well-known/openid-configuration` and has to name exactly this issuer, so the
       * value is kept as configured rather than normalised.
       */
      val issuerUri: String,
      @field:NotBlank val clientId: String,
      /**
       * Unset makes this a public client, which authenticates to the token endpoint by PKCE only.
       * Blank is rejected, as an empty environment variable would otherwise be sent as the secret.
       */
      val clientSecret: String? = null,
      /** Has to include `openid`. */
      val scope: List<String> = DEFAULT_SCOPE,
      /** Name shown to users selecting this registration on the login page. */
      val title: String = DEFAULT_TITLE,
      /** Position of this registration among the login options. */
      val order: Int = 0,
      val usernameMapping: UsernameMapping = UsernameMapping.MAIL_ADDRESS,
      /**
       * Name of the `ExternalLoginLinkingStrategy` which selects the account a new login from this
       * registration is attached to. Unset means that every new login creates an account of its
       * own. No strategy is available unless one is registered, and a name none of the registered
       * ones carries is rejected while the application starts.
       */
      val linkingStrategy: String? = null,
      @field:Valid val claimMapping: ClaimMapping = ClaimMapping(),
      /**
       * Accepts the mail address from the `email` claim only when the `email_verified` claim is
       * `true`. Otherwise the address is treated as absent: it is neither stored, nor offered to a
       * linking strategy, nor used as the username.
       */
      val requireVerifiedMailAddress: Boolean = true
  ) {
    init {
      require(isValidIssuer(issuerUri)) {
        "The issuer-uri of an OIDC registration has to be an absolute http or https URI with a " +
            "host and without query or fragment, but is $issuerUri."
      }
      require(clientSecret == null || clientSecret.isNotBlank()) {
        "The client-secret of an OIDC registration must not be blank. Omit it for a public client."
      }
      require(OPENID_SCOPE in scope) {
        "The scope of an OIDC registration has to include $OPENID_SCOPE, but is $scope."
      }
      val invalidScope = scope.filterNot { SCOPE_TOKEN.matches(it) }
      require(invalidScope.isEmpty()) {
        "The scope of an OIDC registration has to consist of printable ASCII characters other " +
            "than space, \" and \\, but includes $invalidScope."
      }
    }
  }

  data class ClaimMapping(
      /**
       * Claim the account's external login is keyed on. It has to be stable and unique per
       * provider, as `sub` is guaranteed to be: a login whose value changes is taken for a new
       * user, and one arriving with a value another user had is given that user's account. Blank is
       * rejected rather than read as "do not map this", since an empty name would be looked up like
       * any other.
       */
      @field:NotBlank(message = "must name the claim holding the user ID") val id: String = "sub"
  )
}

private fun isValidIssuer(value: String): Boolean {
  val uri =
      try {
        URI(value)
      } catch (_: URISyntaxException) {
        return false
      }
  return uri.scheme?.lowercase() in ISSUER_SCHEMES &&
      !uri.host.isNullOrEmpty() &&
      uri.rawQuery == null &&
      uri.rawFragment == null
}
