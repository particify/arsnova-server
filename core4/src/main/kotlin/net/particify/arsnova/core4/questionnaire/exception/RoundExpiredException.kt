/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.exception

import java.util.UUID

class RoundExpiredException(val id: UUID?) :
    RuntimeException("Answering time for round has run out") {
  constructor() : this(null)
}
