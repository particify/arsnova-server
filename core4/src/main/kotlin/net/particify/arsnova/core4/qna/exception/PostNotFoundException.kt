/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.qna.exception

import java.util.UUID

class PostNotFoundException(val id: UUID?) : RuntimeException("Post not found") {
  constructor() : this(null)
}
