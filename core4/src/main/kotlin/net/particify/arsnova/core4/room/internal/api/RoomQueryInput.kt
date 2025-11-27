/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.room.internal.api

import net.particify.arsnova.core4.room.RoomRole

data class RoomQueryInput(val shortId: String?, val name: String?, val role: RoomRole?)
