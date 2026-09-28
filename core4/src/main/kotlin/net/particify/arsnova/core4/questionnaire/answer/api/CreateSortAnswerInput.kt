/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.answer.api

import java.util.UUID

data class CreateSortAnswerInput(val roundId: UUID, val selectedChoiceIndexes: List<Int>)
