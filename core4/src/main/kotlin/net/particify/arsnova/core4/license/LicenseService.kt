/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.license

interface LicenseService {
  /** Retired licences included, since what was published under them keeps them. */
  fun findById(id: String): License?

  fun findAllById(ids: Collection<String>): List<License>

  /** The licences which can still be chosen, in their order of selection. */
  fun findSelectable(): List<License>
}
