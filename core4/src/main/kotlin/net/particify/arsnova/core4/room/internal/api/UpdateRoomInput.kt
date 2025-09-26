/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.room.internal.api

import java.util.UUID

data class UpdateRoomInput(val id: UUID, val name: String?, val description: String?)
