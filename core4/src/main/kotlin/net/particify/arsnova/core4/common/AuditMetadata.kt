/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.common

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import java.time.Instant
import java.util.UUID
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.annotation.LastModifiedDate

@Embeddable
data class AuditMetadata(
    @CreatedBy @Column(updatable = false) var createdBy: UUID? = null,
    @CreatedDate @Column(updatable = false) var createdAt: Instant? = null,
    @LastModifiedBy @Column(insertable = false) var updatedBy: UUID? = null,
    @LastModifiedDate @Column(insertable = false) var updatedAt: Instant? = null,
)
