/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.license

import liquibase.integration.spring.SpringLiquibase
import net.particify.arsnova.core4.TestcontainersConfiguration
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles

private const val LICENSE_ID = "CC-BY-NC-SA-4.0"

/** The licences are seeded once: operators may retire or reorder them for good. */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration::class)
class LicenseSeedTests {
  @Autowired lateinit var jdbcTemplate: JdbcTemplate
  @Autowired lateinit var licenseService: LicenseService
  @Autowired lateinit var liquibase: SpringLiquibase

  private fun selectable(): Boolean =
      jdbcTemplate.queryForObject(
          "SELECT selectable FROM license.license WHERE id = ?", Boolean::class.java, LICENSE_ID)!!

  private fun setSelectable(selectable: Boolean) {
    jdbcTemplate.update(
        "UPDATE license.license SET selectable = ? WHERE id = ?", selectable, LICENSE_ID)
  }

  @Test
  fun shouldSeedEveryLicence() {
    val ids = setOf("CC0-1.0", "CC-BY-4.0", "CC-BY-SA-4.0", "CC-BY-NC-4.0", "CC-BY-NC-SA-4.0")
    Assertions.assertEquals(ids, licenseService.findAllById(ids).mapNotNull { it.id }.toSet())
  }

  /** Running the changelog again stands in for a restart. */
  @Test
  fun shouldKeepAnOperatorsChangeToASeededLicenceAcrossARestart() {
    setSelectable(false)
    try {
      liquibase.afterPropertiesSet()
      Assertions.assertFalse(selectable())
    } finally {
      setSelectable(true)
    }
  }
}
