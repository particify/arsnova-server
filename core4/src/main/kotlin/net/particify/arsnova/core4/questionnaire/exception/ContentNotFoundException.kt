/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.exception

import java.util.UUID

class ContentNotFoundException(val id: UUID?) : RuntimeException("Content not found") {
  constructor() : this(null)
}
