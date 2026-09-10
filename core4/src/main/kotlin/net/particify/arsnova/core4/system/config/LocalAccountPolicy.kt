/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.config

import java.util.regex.Pattern
import net.particify.arsnova.core4.user.ADMIN_ROLE
import net.particify.arsnova.core4.user.User
import org.springframework.stereotype.Component

private const val WILDCARD_LABEL = "*"
private const val WILDCARD_EXPRESSION = "[^.]+"

/**
 * Decides what may be done with local accounts. The enforced state and the state published to
 * clients are both derived from it, so the two cannot drift apart.
 *
 * The domains are compiled while the application starts rather than when a registration first
 * reaches them: a pattern which never matches turns every registration into an error message about
 * the address rather than about the configuration.
 */
@Component
class LocalAccountPolicy(securityProperties: SecurityProperties) {
  private val properties = securityProperties.localAccount
  private val domainPatterns = properties.allowedMailAddressDomains.map { compile(it) }

  val enabled = properties.enabled

  /** Self-registration needs local accounts, so the outer setting wins over the inner one. */
  val selfRegistrationEnabled = properties.enabled && properties.selfRegistrationEnabled

  /** Domains as configured, for an error message which names the restriction. */
  val allowedMailAddressDomains = properties.allowedMailAddressDomains

  /** Administrative account creation is never gated, so an administrator passes either way. */
  fun mayCreateAccount(actor: User): Boolean = selfRegistrationEnabled || isAdministrator(actor)

  /**
   * An invitation creates the account for someone else, so it carries the domain restriction too.
   * Only an administrator invites past it.
   */
  fun mayInviteMailAddress(inviter: User, mailAddress: String): Boolean =
      isAdministrator(inviter) || isMailAddressAllowed(mailAddress)

  fun isMailAddressAllowed(mailAddress: String): Boolean {
    if (domainPatterns.isEmpty()) {
      return true
    }
    val domain = mailAddress.substringAfterLast('@', "").lowercase()
    return domainPatterns.any { it.matcher(domain).matches() }
  }

  private fun isAdministrator(actor: User) = actor.roles.any { it.name == ADMIN_ROLE }

  private fun compile(domain: String): Pattern =
      Pattern.compile(
          domain.lowercase().split(".").joinToString("\\.") {
            if (it == WILDCARD_LABEL) WILDCARD_EXPRESSION else Pattern.quote(it)
          })
}
