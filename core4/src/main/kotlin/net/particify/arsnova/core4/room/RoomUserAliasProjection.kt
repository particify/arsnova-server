/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.room

import java.util.UUID

/**
 * A persisted alias reduced to the fields needed to display it, so that neither the user nor the
 * room it belongs to has to be loaded. [alias] is null for aliases which are generated from their
 * [seed]; run it through [RoomUserAliasService.localize] to obtain the name to show.
 */
data class RoomUserAliasProjection(
    val userId: UUID,
    val id: UUID?,
    val alias: String?,
    val seed: Int?
)
