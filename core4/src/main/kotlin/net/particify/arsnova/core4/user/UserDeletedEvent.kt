/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user

import java.util.UUID
import org.jmolecules.event.types.DomainEvent

data class UserDeletedEvent(val userId: UUID) : DomainEvent
