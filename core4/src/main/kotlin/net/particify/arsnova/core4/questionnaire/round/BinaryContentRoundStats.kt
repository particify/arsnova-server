/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.round

/**
 * [selectionCounts] holds the yes count followed by the no count. It deliberately mirrors
 * [ChoiceContentRoundStats] in name and shape so clients can present both the same way.
 */
class BinaryContentRoundStats(val selectionCounts: List<Int>)
