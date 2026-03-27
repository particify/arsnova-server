/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.qna.event

import java.util.UUID
import org.jmolecules.event.annotation.DomainEvent

@DomainEvent
class QnaDuplicatedEvent(val originalQnaId: UUID, val duplicatedQnaId: UUID) : QnaEvent
