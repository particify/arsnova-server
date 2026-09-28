/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.answer

import java.util.UUID
import net.particify.arsnova.core4.room.RoomUserAlias
import net.particify.arsnova.core4.room.RoomUserAliasProjection

/**
 * A participant's standing in a questionnaire as it is computed once for all readers. [alias] is
 * null for users who have not set an alias for the room; those entries are still part of the
 * leaderboard. It is left unlocalized so that readers of different locales can share the
 * computation. [currentResult] is only populated when the leaderboard is scoped to a single
 * content.
 */
data class RawLeaderboardEntry(
    val userId: UUID,
    val alias: RoomUserAliasProjection?,
    val score: Int,
    val position: Int,
    val currentResult: LeaderboardCurrentResult?
)

/** A [RawLeaderboardEntry] with a single reader's locale applied to its alias. */
data class LeaderboardEntry(
    val userAlias: RoomUserAlias?,
    val score: Int,
    val position: Int,
    val currentResult: LeaderboardCurrentResult?
)

data class LeaderboardCurrentResult(val points: Int, val durationMs: Int, val correct: Boolean)

/**
 * A section of the ranking. [ownEntry] carries the reader's own standing whenever it is not part of
 * [entries], since a participant cannot derive their position from a list they were cut from.
 */
data class LeaderboardPage(
    val entries: List<LeaderboardEntry>,
    val ownEntry: LeaderboardEntry?,
    val totalCount: Int
)

/** A score which has been placed in the ranking. */
data class RankedScore(val userId: UUID, val score: Int, val position: Int)
