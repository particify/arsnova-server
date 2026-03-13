/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.qna

import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.util.UUID
import net.particify.arsnova.core4.common.AuditMetadataUuidV7
import net.particify.arsnova.core4.common.UuidV7Generator
import org.hibernate.annotations.Formula
import org.hibernate.annotations.JdbcType
import org.hibernate.dialect.type.PostgreSQLEnumJdbcType

@Entity
@Table(schema = "qna")
class Post(
    @Id @UuidV7Generator var id: UUID? = null,
    @Version var version: Int? = 0,
    @ManyToOne var qna: Qna? = null,
    var body: String? = null,
    var correct: Boolean? = null,
    var favorite: Boolean = false,
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "post_tag",
        schema = "qna",
        joinColumns = [JoinColumn(name = "post_id")],
        inverseJoinColumns = [JoinColumn(name = "tag_id")])
    var tags: MutableSet<Tag> = mutableSetOf(),
    @OneToMany(mappedBy = "post", fetch = FetchType.EAGER)
    var replies: MutableSet<Reply> = mutableSetOf(),
    @JdbcType(PostgreSQLEnumJdbcType::class)
    var moderationState: ModerationState = ModerationState.PENDING,
    @Formula("(SELECT COALESCE(SUM(v.value), 0) FROM qna.vote v WHERE v.post_id = id)")
    var score: Int = 0,
    @Embedded val auditMetadata: AuditMetadataUuidV7 = AuditMetadataUuidV7()
)
