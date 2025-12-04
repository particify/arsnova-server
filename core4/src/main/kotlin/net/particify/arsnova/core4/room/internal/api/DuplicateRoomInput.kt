/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.room.internal.api

import java.util.UUID

data class DuplicateRoomInput(val id: UUID, val newName: String)
