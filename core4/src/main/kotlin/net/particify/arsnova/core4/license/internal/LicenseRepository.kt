/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.license.internal

import net.particify.arsnova.core4.license.License
import org.springframework.data.jpa.repository.JpaRepository

interface LicenseRepository : JpaRepository<License, String> {
  fun findBySelectableTrueOrderByPosition(): List<License>
}
