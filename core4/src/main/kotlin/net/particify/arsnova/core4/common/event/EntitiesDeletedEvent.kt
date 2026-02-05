/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.common.event

/**
 * Event for entity deletions triggered by deletion of their parent entity. [entityId] holds the
 * parent entity's ID.
 */
interface EntitiesDeletedEvent<ID> : EntityEvent<ID> {
  val count: Int
}
