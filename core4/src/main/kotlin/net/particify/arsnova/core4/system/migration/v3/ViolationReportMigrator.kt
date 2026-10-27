/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.migration.v3

import java.security.SecureRandom
import java.util.UUID
import java.util.random.RandomGenerator
import net.particify.arsnova.core4.common.AuditMetadataUuidV7
import net.particify.arsnova.core4.system.migration.v3.ViolationReport as ViolationReportV3
import net.particify.arsnova.core4.user.internal.UserServiceImpl
import net.particify.arsnova.core4.violationreport.ViolationReport
import net.particify.arsnova.core4.violationreport.ViolationReportReason
import net.particify.arsnova.core4.violationreport.event.ViolationReportDecision
import net.particify.arsnova.core4.violationreport.internal.ViolationReportRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

private val logger = LoggerFactory.getLogger(ViolationReportMigrator::class.java)
private val TARGET_TYPES = mapOf("ContentGroupTemplate" to "TEMPLATE", "Room" to "ROOM")

/**
 * Handles migration of violation reports from v3. Nothing references a report, so reports get new
 * UUIDv7 ids which carry their v3 creation time. Reports are kept whether or not their target
 * exists, so a template report only resolves if templates are migrated with their ids converted
 * through [UuidHelper.stringToUuid], as the target ids are.
 */
@Component
@ConditionalOnBooleanProperty(name = ["persistence.v3-migration.enabled"])
class ViolationReportMigrator(
    private val couchdbMigrator: CouchdbMigrator,
    private val userService: UserServiceImpl,
    private val violationReportRepository: ViolationReportRepository,
) {
  private val randomGenerator = SecureRandom()

  @Transactional
  fun migrateViolationReports() {
    logger.info("Migrating ViolationReport data...")
    val count = violationReportRepository.count()
    if (count > 0) {
      logger.warn("ViolationReport table not empty ({}), skipping migration.", count)
      return
    }
    val ghostUserId = userService.getOrCreateGhostUser().id!!
    couchdbMigrator.migrate<ViolationReportV3> {
      var userId = UuidHelper.stringToUuid(it.creatorId)!!
      if (!userService.existsById(userId)) {
        logger.info(
            "Creator {} for violation report {} not found. Using ghost user.", userId, it.id)
        userId = ghostUserId
      }
      toViolationReport(it, userId, randomGenerator)
    }
  }
}

/** Maps [report] to a core4 report created by [userId], or null for an unknown target type. */
internal fun toViolationReport(
    report: ViolationReportV3,
    userId: UUID,
    randomGenerator: RandomGenerator
): ViolationReport? {
  val targetType = TARGET_TYPES[report.targetType]
  if (targetType == null) {
    logger.warn(
        "Unknown target type {} for violation report {}. Skipping report creation.",
        report.targetType,
        report.id)
    return null
  }
  return ViolationReport(
      id = UuidHelper.generateUuidV7(report.creationTimestamp, randomGenerator),
      auditMetadata = AuditMetadataUuidV7(updatedAt = report.updateTimestamp, createdBy = userId),
      targetType = targetType,
      targetId = UuidHelper.stringToUuid(report.targetId),
      reason = ViolationReportReason.valueOf(report.reason),
      description = report.description,
      decision = report.decision?.let { ViolationReportDecision.valueOf(it) })
}
