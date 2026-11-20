/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.violationreport.event

import java.util.UUID
import net.particify.arsnova.core4.common.event.EntityEvent
import org.jmolecules.event.annotation.DomainEvent

/**
 * Published within the transaction storing the decision, so a listener failing to act on a
 * [ViolationReportDecision.REMOVAL] rolls the decision back.
 */
@DomainEvent
data class ViolationReportDecidedEvent(
    override val id: UUID,
    val targetType: String,
    val targetId: UUID,
    val decision: ViolationReportDecision,
) : ViolationReportEvent, EntityEvent<UUID>
