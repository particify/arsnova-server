/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.migration.v3

import java.sql.ResultSet
import java.util.UUID
import org.springframework.jdbc.core.RowMapper

class QnaVoteRowMapper : RowMapper<QnaVoteRowMapper.Vote> {
  override fun mapRow(rs: ResultSet, rowNum: Int) =
      Vote(userId = rs.getObject("user_id", UUID::class.java), vote = rs.getInt("vote"))

  data class Vote(val userId: UUID, val vote: Int)
}
