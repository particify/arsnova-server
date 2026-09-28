/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.round

/**
 * The points every answer assigned, summed per option, in the content's own option order and
 * zero-filled to its option count.
 */
data class PrioritizationContentRoundStats(val assignedPoints: List<Int>)
