/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.qna.event

import java.util.UUID
import net.particify.arsnova.core4.common.event.EntityCreatedEvent
import org.jmolecules.event.annotation.DomainEvent

@DomainEvent
data class PostCreatedEvent(override val id: UUID) : PostEvent, EntityCreatedEvent<UUID>
