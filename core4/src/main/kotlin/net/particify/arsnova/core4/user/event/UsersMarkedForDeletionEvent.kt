/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user.event

import org.jmolecules.event.annotation.DomainEvent

@DomainEvent
data class UsersMarkedForDeletionEvent(val kind: Kind, val count: Int) : UserEvent {
  enum class Kind {
    INACTIVE_UNVERIFIED,
    INACTIVE_VERIFIED
  }
}
