/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.qna.exception

import java.util.UUID

class QnaNotFoundException(val id: UUID?) : RuntimeException("Qna not found") {
  constructor() : this(null)
}
