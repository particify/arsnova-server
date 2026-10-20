/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.round

/**
 * Counts per displayed option, most positive first and zero-filled for options nobody picked. Five
 * entries when the content has its neutral option, four without.
 */
data class ScaleContentRoundStats(val counts: List<Int>)
