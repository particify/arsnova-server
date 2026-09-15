/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.qna.exception

import java.util.UUID

class ReplyNotFoundException(val id: UUID?) : RuntimeException("Reply not found") {
  constructor() : this(null)
}
