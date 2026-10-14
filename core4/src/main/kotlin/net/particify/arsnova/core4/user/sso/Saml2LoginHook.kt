/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user.sso

import java.util.UUID
import net.particify.arsnova.core4.user.User

/**
 * Takes part in every SAML login whose response has passed validation, before and after the account
 * is provisioned. Every hook in the application context is called, in the order Spring gives an
 * injected list.
 *
 * The attributes are those of the assertion, keyed by attribute name. They are personal data, so
 * their values must not be logged.
 */
interface Saml2LoginHook {
  /**
   * Called before the account is looked up, created or updated, so nothing has been written yet.
   * Throwing a `Saml2AuthenticationException` refuses the login, and its error code is shown to the
   * user.
   */
  fun beforeProvisioning(registrationId: UUID, attributes: Map<String, List<Any>>) {}

  /**
   * Called once the account has been created or updated, inside the same transaction. Changes made
   * to [user] are persisted with it and already apply to the session the login starts. Throwing
   * rolls the provisioning back and fails the login.
   */
  fun afterProvisioning(user: User, registrationId: UUID, attributes: Map<String, List<Any>>) {}
}
