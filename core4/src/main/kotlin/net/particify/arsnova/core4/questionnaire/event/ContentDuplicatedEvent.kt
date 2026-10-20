/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.event

import java.util.UUID
import org.jmolecules.event.annotation.DomainEvent

@DomainEvent
class ContentDuplicatedEvent(val originalContentId: UUID, val duplicatedContentId: UUID) :
    ContentEvent
