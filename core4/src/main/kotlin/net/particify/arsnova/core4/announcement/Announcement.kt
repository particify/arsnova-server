/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.announcement

import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.util.UUID
import net.particify.arsnova.core4.common.AuditMetadata
import net.particify.arsnova.core4.room.Room

@Entity
@Table(schema = "announcement")
class Announcement(
    @Id @GeneratedValue(strategy = GenerationType.UUID) var id: UUID? = null,
    @Version var version: Int? = 0,
    @ManyToOne var room: Room? = null,
    var title: String? = null,
    var body: String? = null,
    @Embedded val auditMetadata: AuditMetadata = AuditMetadata(),
)
