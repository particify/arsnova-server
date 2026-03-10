/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.migration.v3

import java.sql.ResultSet
import org.springframework.jdbc.core.RowMapper

class QnaCommentSettingsRowMapper : RowMapper<QnaCommentSettingsRowMapper.CommentSettings> {
  override fun mapRow(rs: ResultSet, rowNum: Int) =
      CommentSettings(
          directSend = rs.getBoolean("direct_send"),
          readonly = rs.getBoolean("readonly"),
          disabled = rs.getBoolean("disabled"))

  data class CommentSettings(
      val directSend: Boolean = true,
      val readonly: Boolean = false,
      val disabled: Boolean = true
  )
}
