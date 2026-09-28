/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.exception

import java.util.UUID

class AnswerNotFoundException(val id: UUID?) : RuntimeException("Answer not found") {
  constructor() : this(null)
}
