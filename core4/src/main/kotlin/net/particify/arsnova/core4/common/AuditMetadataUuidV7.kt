/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.common

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import java.time.Instant
import java.util.UUID
import org.hibernate.annotations.Formula
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.annotation.LastModifiedDate

@Embeddable
data class AuditMetadataUuidV7(
    @CreatedBy @Column(updatable = false) var createdBy: UUID? = null,
    /* @Formula is used instead of the more appropriate @GeneratedColumn to work around a Hibernate bug.
     * See https://hibernate.atlassian.net/browse/HHH-16957. */
    // @GeneratedColumn("uuid_extract_timestamp(id)") var createdAt: Instant? = null,
    @Formula("uuid_extract_timestamp(id)") var createdAt: Instant? = Instant.now(),
    @LastModifiedBy @Column(insertable = false) var updatedBy: UUID? = null,
    @LastModifiedDate @Column(insertable = false) var updatedAt: Instant? = null,
)
