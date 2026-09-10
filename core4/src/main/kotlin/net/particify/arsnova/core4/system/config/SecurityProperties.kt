/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.config

import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.net.URL
import java.time.Duration
import org.hibernate.validator.constraints.Length
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

private const val BCRYPT_MIN_LOG_ROUNDS = 4L
private const val BCRYPT_MAX_LOG_ROUNDS = 31L
private const val DOMAIN_LABEL = "(\\*|[a-z0-9]([a-z0-9-]*[a-z0-9])?)"
private val DOMAIN_PATTERN = Regex("$DOMAIN_LABEL(\\.$DOMAIN_LABEL)*", RegexOption.IGNORE_CASE)

@ConfigurationProperties(prefix = "security")
@Validated
data class SecurityProperties(
    @field:Valid val password: Password,
    @field:Valid val jwt: Jwt,
    @field:Valid val challenge: Challenge,
    @field:Valid val login: Login,
    @field:Valid val localAccount: LocalAccount,
    @field:Valid val roomCreatorRole: RoomCreatorRole,
    @field:NotBlank val authorizeUriHeader: String,
    @field:NotBlank val authorizeUriPrefix: String,
) {
  data class Password(
      @field:Min(BCRYPT_MIN_LOG_ROUNDS) @field:Max(BCRYPT_MAX_LOG_ROUNDS) val bcryptStrength: Int,
  )

  data class Jwt(
      @field:Length(min = 32) val secret: String,
      @field:NotBlank val issuer: String,
      val idpIssuer: URL?,
      val validityPeriod: Duration
  )

  data class Challenge(
      @field:Positive val validitySeconds: Long,
      val algorithm: String,
      @field:Size(min = 32) val secret: String,
      @field:Positive val iterationCost: Int,
      @field:Positive val maxIterations: Int,
  )

  data class Login(
      @field:Positive val attemptLimit: Long,
      val attemptWindow: Duration,
  )

  data class LocalAccount(
      /**
       * Whether accounts with a username and password exist at all. It gates logging in with one,
       * creating one and every operation on one, so [selfRegistrationEnabled] is without effect
       * while it is `false`.
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

  data class RoomCreatorRole(
      /**
       * Which accounts the room creator role is assigned to automatically. It does not govern roles
       * assigned to an account by hand.
       *
       * [AutoAssignment.VERIFIED_ACCOUNTS] withholds the role from every account without a
       * username. A local account whose registration has not been confirmed yet is unverified in
       * that sense, and so is an account whose external login could not be assigned a username
       * because another account already held it.
       */
      val autoAssignTo: AutoAssignment
  ) {
    enum class AutoAssignment {
      ALL_ACCOUNTS,
      VERIFIED_ACCOUNTS,
      NONE
    }
  }
}
