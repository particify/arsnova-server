/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.answer

import java.util.UUID

/** The fields of an answer the leaderboard shows as its user's current result. */
data class LeaderboardAnswerProjection(
    val userId: UUID,
    val points: Int?,
    val durationMs: Int?,
    val result: AnswerResultState?
)
