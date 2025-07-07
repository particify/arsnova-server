/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user

import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.time.Instant
import java.util.UUID
import net.particify.arsnova.core4.common.AuditMetadata
import net.particify.arsnova.core4.common.UuidGenerator
import net.particify.arsnova.core4.user.internal.Role
import org.hibernate.annotations.JdbcType
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.dialect.PostgreSQLEnumJdbcType
import org.hibernate.type.SqlTypes
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

@Entity
@Table(schema = "user")
@Suppress("LongParameterList")
class User(
    @Id @UuidGenerator var id: UUID? = null,
    @Version var version: Int? = 0,
    @JdbcType(PostgreSQLEnumJdbcType::class) var type: Type? = Type.ACCOUNT,
    @JvmField var username: String? = null,
    @JvmField var password: String? = null,
    var mailAddress: String? = null,
    var givenName: String? = null,
    var surname: String? = null,
    var enabled: Boolean? = true,
    @ManyToMany
    @JoinTable(
        schema = "user",
        name = "user_role_mapping",
        joinColumns = [JoinColumn(name = "user_id")],
        inverseJoinColumns = [JoinColumn(name = "role_id")],
    )
    val roles: List<Role> = mutableListOf(),
    @JdbcTypeCode(SqlTypes.JSON) val settings: MutableMap<String, Any> = mutableMapOf(),
    var announcementsReadAt: Instant? = null,
    @Embedded val auditMetadata: AuditMetadata = AuditMetadata(),
) : UserDetails {
  override fun getAuthorities(): Set<GrantedAuthority> =
      roles.map { SimpleGrantedAuthority("ROLE_" + it.name) }.toSet()

  override fun getPassword(): String? = password

  override fun getUsername(): String? = username

  override fun isEnabled(): Boolean = enabled ?: false

  enum class Type {
    ACCOUNT,
    GUEST,
    DELETED
  }
}
