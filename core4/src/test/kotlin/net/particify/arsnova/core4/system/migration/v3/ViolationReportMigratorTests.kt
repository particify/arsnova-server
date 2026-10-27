/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.migration.v3

import java.security.SecureRandom
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import net.particify.arsnova.core4.TestcontainersConfiguration
import net.particify.arsnova.core4.violationreport.ViolationReportReason
import net.particify.arsnova.core4.violationreport.event.ViolationReportDecision
import net.particify.arsnova.core4.violationreport.internal.ViolationReportRepository
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.data.repository.findByIdOrNull
import org.springframework.test.context.ActiveProfiles

/** An account from the `dev` Liquibase fixtures. */
private val USER_ID = UUID.fromString("9c778494-9b52-424d-8c7f-f4936629facb")
private val CREATED_AT = Instant.parse("2024-01-02T03:04:05.678912Z")
private val UPDATED_AT = Instant.parse("2024-02-03T04:05:06Z")

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration::class)
class ViolationReportMigratorTests {
  @Autowired lateinit var violationReportRepository: ViolationReportRepository

  private val randomGenerator = SecureRandom()

  private fun report(targetType: String, decision: String? = null) =
      ViolationReport(
          id = "0b1c2d3e4f5a6b7c8d9e0f1a2b3c4d5e",
          creationTimestamp = CREATED_AT,
          updateTimestamp = UPDATED_AT,
          targetType = targetType,
          targetId = "f0e1d2c3b4a5968778695a4b3c2d1e0f",
          reason = "HATE_SPEECH",
          description = "Offensive",
          creatorId = "00112233445566778899aabbccddeeff",
          decision = decision)

  @Test
  fun shouldMapTemplateReportWithConvertedTargetId() {
    val migrated =
        toViolationReport(report("ContentGroupTemplate", "REMOVAL"), USER_ID, randomGenerator)!!
    Assertions.assertEquals("TEMPLATE", migrated.targetType)
    Assertions.assertEquals(
        UUID.fromString("f0e1d2c3-b4a5-9687-7869-5a4b3c2d1e0f"), migrated.targetId)
    Assertions.assertEquals(ViolationReportReason.HATE_SPEECH, migrated.reason)
    Assertions.assertEquals("Offensive", migrated.description)
    Assertions.assertEquals(ViolationReportDecision.REMOVAL, migrated.decision)
    Assertions.assertEquals(USER_ID, migrated.auditMetadata.createdBy)
    Assertions.assertEquals(UPDATED_AT, migrated.auditMetadata.updatedAt)
    Assertions.assertEquals(7, migrated.id!!.version())
  }

  @Test
  fun shouldMapUndecidedRoomReport() {
    val migrated = toViolationReport(report("Room"), USER_ID, randomGenerator)!!
    Assertions.assertEquals("ROOM", migrated.targetType)
    Assertions.assertNull(migrated.decision)
  }

  @Test
  fun shouldSkipReportOfUnknownTargetType() {
    Assertions.assertNull(toViolationReport(report("Content"), USER_ID, randomGenerator))
  }

  /** The id carries the creation time, at the millisecond precision of UUIDv7. */
  @Test
  fun shouldKeepCreationTimeInId() {
    val migrated = toViolationReport(report("Room"), USER_ID, randomGenerator)!!
    violationReportRepository.save(migrated)
    val persisted = violationReportRepository.findByIdOrNull(migrated.id!!)!!
    Assertions.assertEquals(
        CREATED_AT.truncatedTo(ChronoUnit.MILLIS), persisted.auditMetadata.createdAt)
  }
}
