/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.room.internal.api

import java.util.UUID

data class SetRoomUserAliasInput(val roomId: UUID, val alias: String?, val seed: Int?)
