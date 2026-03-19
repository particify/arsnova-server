/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.qna.internal.api

import java.util.UUID

data class CreatePostInput(val qnaId: UUID, val body: String, val tagIds: List<UUID>?)
