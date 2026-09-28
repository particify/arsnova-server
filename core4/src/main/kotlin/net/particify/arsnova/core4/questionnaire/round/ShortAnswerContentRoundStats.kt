/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.round

/**
 * Answer counts per distinct text, descending by count. Answers differing only in case, punctuation
 * or spacing share an entry, labelled with the content's own spelling of the correct term where one
 * matches and with the most common variant otherwise.
 */
data class ShortAnswerContentRoundStats(val texts: List<String>, val counts: List<Int>)
