/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.violationreport.internal

import jakarta.transaction.Transactional
import java.util.UUID
import net.particify.arsnova.core4.common.exception.InvalidInputException
import net.particify.arsnova.core4.violationreport.ViolationReport
import net.particify.arsnova.core4.violationreport.event.ViolationReportDecidedEvent
import net.particify.arsnova.core4.violationreport.event.ViolationReportDecision
import net.particify.arsnova.core4.violationreport.exception.ViolationReportAlreadyDecidedException
import net.particify.arsnova.core4.violationreport.exception.ViolationReportNotFoundException
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

private const val MAX_DESCRIPTION_LENGTH = 500

@Service
class ViolationReportServiceImpl(
    private val violationReportRepository: ViolationReportRepository,
    private val applicationEventPublisher: ApplicationEventPublisher
) : ViolationReportRepository by violationReportRepository {
  fun create(report: ViolationReport): ViolationReport {
    val description = report.description?.trim().orEmpty()
    if (description.isEmpty() || description.length > MAX_DESCRIPTION_LENGTH) {
      throw InvalidInputException(
          "Violation report descriptions must have 1 to $MAX_DESCRIPTION_LENGTH characters")
    }
    report.description = description
    return violationReportRepository.save(report)
  }

  /**
   * A decision is final. A [ViolationReportDecision.REMOVAL] also closes the other open reports on
   * the same target, since the target is gone afterwards.
   */
  @Transactional
  fun decide(id: UUID, decision: ViolationReportDecision): ViolationReport {
    val report =
        violationReportRepository.findByIdOrNull(id) ?: throw ViolationReportNotFoundException(id)
    if (report.decision != null) {
      throw ViolationReportAlreadyDecidedException(id)
    }
    val targetType = report.targetType!!
    val targetId = report.targetId!!
    report.decision = decision
    val persistedReport = violationReportRepository.save(report)
    if (decision == ViolationReportDecision.REMOVAL) {
      val siblings =
          violationReportRepository.findByTargetTypeAndTargetIdAndDecisionIsNull(
              targetType, targetId)
      siblings.forEach { it.decision = ViolationReportDecision.REMOVAL }
      violationReportRepository.saveAll(siblings)
    }
    applicationEventPublisher.publishEvent(
        ViolationReportDecidedEvent(id, targetType, targetId, decision))
    return persistedReport
  }

  @Deprecated(
      "Deprecated by base implementation", replaceWith = ReplaceWith("deleteAllInBatch(entities)"))
  override fun deleteInBatch(entities: Iterable<ViolationReport>) =
      violationReportRepository.deleteInBatch(entities)
}
