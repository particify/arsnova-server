/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.round

/**
 * Answer counts per distinct ordering, descending by count. Every ordering that was submitted is
 * included: the client picks which ones to show and needs the rest to aggregate them.
 */
data class SortContentRoundStats(val combinations: List<SortCombination>)

data class SortCombination(val selectedChoiceIndexes: List<Int>, val count: Int)
