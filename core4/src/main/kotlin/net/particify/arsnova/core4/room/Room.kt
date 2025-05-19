/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.room

import jakarta.persistence.CascadeType
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.util.UUID
import net.particify.arsnova.core4.common.AuditMetadata

@Entity
@Table(schema = "room")
@Suppress("LongParameterList")
class Room(
    @Id @GeneratedValue(strategy = GenerationType.UUID) var id: UUID? = null,
    @Version var version: Int? = 0,
    val shortId: Int? = null,
    var name: String? = null,
    var description: String? = "",
    @OneToMany(mappedBy = "room", cascade = [CascadeType.ALL])
    val userRoles: MutableSet<Membership> = mutableSetOf(),
    @Embedded val auditMetadata: AuditMetadata = AuditMetadata(),
) {
  companion object {
    const val SHORT_ID_LENGTH = 8
  }
}
