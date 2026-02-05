/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.announcement.event

import java.util.UUID
import net.particify.arsnova.core4.common.event.EntityDeletedEvent
import org.jmolecules.event.annotation.DomainEvent

@DomainEvent
data class AnnouncementDeletedEvent(override val entityId: UUID) :
    AnnouncementEvent, EntityDeletedEvent<UUID>
