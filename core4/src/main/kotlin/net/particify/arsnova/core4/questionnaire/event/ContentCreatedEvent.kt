/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.event

import java.util.UUID
import net.particify.arsnova.core4.common.event.EntityCreatedEvent
import org.jmolecules.event.annotation.DomainEvent

@DomainEvent
data class ContentCreatedEvent(override val id: UUID) : ContentEvent, EntityCreatedEvent<UUID>
