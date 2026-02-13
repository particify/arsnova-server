/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user.event

import java.util.UUID
import net.particify.arsnova.core4.common.event.EntityEvent
import org.jmolecules.event.annotation.DomainEvent

@DomainEvent data class UserMailVerifiedEvent(override val id: UUID) : UserEvent, EntityEvent<UUID>
