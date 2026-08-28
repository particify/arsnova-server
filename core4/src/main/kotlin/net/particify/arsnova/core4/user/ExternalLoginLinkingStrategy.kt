/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user

import java.util.UUID

/** Selects the account which a new external login is attached to. */
interface ExternalLoginLinkingStrategy {
  /**
   * Identifies this strategy where a login provider registration selects one. Names have to be
   * distinct across all registered strategies.
   */
  val name: String

  /**
   * Consulted only when no external login exists for [providerId] and [externalId] yet, so an
   * existing login is never moved to another account. Returning `null` means that a new account is
   * created, which is also what happens for a registration which selects no strategy.
   *
   * @param mailAddress the address as asserted by the provider. It may already be held by another
   *   account and is then not stored on the account this returns.
   */
  fun findLinkTarget(providerId: UUID, externalId: String, mailAddress: String?): User?
}
