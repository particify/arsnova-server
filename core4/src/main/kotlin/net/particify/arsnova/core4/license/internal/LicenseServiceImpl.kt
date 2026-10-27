/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.license.internal

import net.particify.arsnova.core4.license.License
import net.particify.arsnova.core4.license.LicenseService
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class LicenseServiceImpl(private val licenseRepository: LicenseRepository) : LicenseService {
  override fun findById(id: String): License? = licenseRepository.findByIdOrNull(id)

  override fun findAllById(ids: Collection<String>): List<License> =
      licenseRepository.findAllById(ids)

  override fun findSelectable(): List<License> =
      licenseRepository.findBySelectableTrueOrderByPosition()
}
