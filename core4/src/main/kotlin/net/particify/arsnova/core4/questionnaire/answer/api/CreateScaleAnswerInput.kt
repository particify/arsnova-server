/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.answer.api

import java.util.UUID

data class CreateScaleAnswerInput(val roundId: UUID, val rating: Int)
