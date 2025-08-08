/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.qna.event

import java.util.UUID
import net.particify.arsnova.core4.common.event.EntitiesDeletedEvent
import org.jmolecules.event.annotation.DomainEvent

@DomainEvent
data class VotesDeletedEvent(override val id: UUID, override val count: Int) :
    VoteEvent, EntitiesDeletedEvent<UUID>
