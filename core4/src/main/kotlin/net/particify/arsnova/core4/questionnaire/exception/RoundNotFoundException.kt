/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.exception

import java.util.UUID

class RoundNotFoundException(val id: UUID?) : RuntimeException("Round not found") {
  constructor() : this(null)
}
