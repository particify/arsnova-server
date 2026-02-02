/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.room

import jakarta.persistence.CascadeType
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.util.UUID
import net.particify.arsnova.core4.common.AuditMetadata
import net.particify.arsnova.core4.common.LanguageIso639
import net.particify.arsnova.core4.common.UuidGenerator
import net.particify.arsnova.core4.room.event.RoomEntityEventDispatcher
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

@Entity
@Table(schema = "room")
@EntityListeners(RoomEntityEventDispatcher::class)
@Suppress("LongParameterList")
class Room(
    @Id @UuidGenerator var id: UUID? = null,
    @Version var version: Int? = 0,
    val shortId: Int? = null,
    var name: String? = null,
    var description: String? = "",
    @field:LanguageIso639 var language: String? = null,
    var focusModeEnabled: Boolean? = false,
    @OneToMany(mappedBy = "room", cascade = [CascadeType.ALL])
    val userRoles: MutableSet<Membership> = mutableSetOf(),
    @JdbcTypeCode(SqlTypes.JSON) val metadata: MutableMap<String, Any> = mutableMapOf(),
    @Embedded val auditMetadata: AuditMetadata = AuditMetadata(),
) {
  companion object {
    const val SHORT_ID_LENGTH = 8
  }

  fun copy(shortId: Int, name: String? = null): Room {
    return Room(
        shortId = shortId,
        name = name ?: this.name,
        description = description,
        language = language,
        focusModeEnabled = focusModeEnabled)
  }
}
