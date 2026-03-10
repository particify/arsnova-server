/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.migration.v3

import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID
import org.springframework.jdbc.core.RowMapper

class QnaCommentRowMapper : RowMapper<QnaCommentRowMapper.Comment> {
  override fun mapRow(rs: ResultSet, rowNum: Int) =
      Comment(
          id = rs.getObject("id", UUID::class.java),
          body = rs.getString("body") ?: "",
          answer = rs.getString("answer"),
          ack = rs.getBoolean("ack"),
          correct = rs.getInt("correct"),
          favorite = rs.getBoolean("favorite"),
          tag = rs.getString("tag"),
          creatorId = rs.getObject("creator_id", UUID::class.java),
          timestamp = rs.getObject("timestamp", Timestamp::class.java).toInstant())

  data class Comment(
      val id: UUID,
      val body: String,
      val answer: String?,
      val ack: Boolean,
      val correct: Int,
      val favorite: Boolean,
      val tag: String? = null,
      val creatorId: UUID,
      val timestamp: Instant
  )
}
