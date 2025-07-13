/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user.internal

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import jakarta.persistence.Version
import java.time.Instant
import java.util.UUID
import net.particify.arsnova.core4.common.AuditMetadata
import net.particify.arsnova.core4.user.User

@Entity
@Table(
    schema = "user",
    uniqueConstraints =
        [
            UniqueConstraint(columnNames = ["user", "provider_id"]),
            UniqueConstraint(columnNames = ["provider_id", "external_id"])])
@Suppress("LongParameterList")
class ExternalLogin(
    @Id @GeneratedValue(strategy = GenerationType.UUID) var id: UUID? = null,
    @Version var version: Int? = 0,
    @ManyToOne(cascade = [CascadeType.PERSIST])
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    var user: User? = null,
    @Column(updatable = false) var providerId: UUID? = null,
    @Column(updatable = false) var externalId: String? = null,
    var lastLoginAt: Instant? = null,
    @Embedded val auditMetadata: AuditMetadata = AuditMetadata(),
)
