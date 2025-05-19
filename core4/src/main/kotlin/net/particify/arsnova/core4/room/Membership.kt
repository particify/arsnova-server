/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.room

import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.ManyToOne
import jakarta.persistence.MapsId
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID
import net.particify.arsnova.core4.user.User
import org.hibernate.annotations.JdbcType
import org.hibernate.dialect.PostgreSQLEnumJdbcType
import org.springframework.data.annotation.Version

@Entity
@Table(schema = "room")
class Membership(
    @EmbeddedId var id: RoomUserId? = RoomUserId(),
    @Version var version: Int? = 0,
    @ManyToOne @MapsId("roomId") var room: Room? = null,
    @ManyToOne @MapsId("userId") var user: User? = null,
    @JdbcType(PostgreSQLEnumJdbcType::class) var role: RoomRole? = null,
    var lastActivityAt: Instant? = null,
) {
  @Embeddable data class RoomUserId(var roomId: UUID? = null, var userId: UUID? = null)
}
