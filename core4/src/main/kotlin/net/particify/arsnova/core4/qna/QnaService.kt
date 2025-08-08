/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.qna

import java.util.UUID

interface QnaService {
  fun create(roomId: UUID, topic: String? = null)
}
