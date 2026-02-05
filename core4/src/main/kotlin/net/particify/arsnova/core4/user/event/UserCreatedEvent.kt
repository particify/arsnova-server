/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user.event

import java.util.UUID
import net.particify.arsnova.core4.common.event.EntityCreatedEvent
import org.jmolecules.event.annotation.DomainEvent

@DomainEvent
data class UserCreatedEvent(override val entityId: UUID) : UserEvent, EntityCreatedEvent<UUID>
