/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user

import java.util.UUID

/**
 * Takes part in every login through a single sign-on registration whose response has passed
 * validation, before and after the account is provisioned. Directory (LDAP) logins do not call it.
 * Every hook in the application context is called, in the order Spring gives an injected list.
 *
 * The attributes are those the identity provider released about the user, keyed by attribute name.
 * They are personal data, so their values must not be logged.
 */
interface ExternalLoginHook {
  /**
   * Called before the account is looked up, created or updated, so nothing has been written yet.
   * Throwing an [ExternalLoginRefusedException] refuses the login, and its error code is shown to
   * the user.
   */
  fun beforeProvisioning(registrationId: UUID, attributes: Map<String, List<Any>>) {}

  /**
   * Called once the account has been created or updated, inside the same transaction. Changes made
   * to [user] are persisted with it and already apply to the session the login starts. Throwing
   * rolls the provisioning back and fails the login.
   */
  fun afterProvisioning(user: User, registrationId: UUID, attributes: Map<String, List<Any>>) {}
}
