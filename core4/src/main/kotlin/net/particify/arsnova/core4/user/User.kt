/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user

import jakarta.persistence.CascadeType
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.time.Instant
import java.util.UUID
import net.particify.arsnova.core4.common.AuditMetadata
import net.particify.arsnova.core4.common.UuidGenerator
import net.particify.arsnova.core4.user.internal.ExternalLogin
import net.particify.arsnova.core4.user.internal.VERIFICATION_MAX_ERRORS
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.annotations.SoftDelete
import org.hibernate.type.SqlTypes
import org.springframework.security.core.AuthenticatedPrincipal
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

@Entity
@Table(schema = "user")
@SoftDelete
@Suppress("LongParameterList")
class User(
    @Id @UuidGenerator var id: UUID? = null,
    @Version var version: Int? = 0,
    @JvmField var username: String? = null,
    @JvmField var password: String? = null,
    var mailAddress: String? = null,
    var unverifiedMailAddress: String? = null,
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
    val roles: MutableList<Role> = mutableListOf(),
    @JdbcTypeCode(SqlTypes.JSON) val settings: MutableMap<String, Any> = mutableMapOf(),
    var tokenVersion: Int? = 1,
    var passwordChangedAt: Instant? = null,
    var announcementsReadAt: Instant? = null,
    var verificationCode: Int? = null,
    var verificationErrors: Int? = 0,
    var verificationExpiresAt: Instant? = null,
    @Embedded val auditMetadata: AuditMetadata = AuditMetadata(),
    @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL])
    val externalLogins: MutableList<ExternalLogin> = mutableListOf(),
) : AuthenticatedPrincipal, UserDetails {
  override fun getName() = username

  override fun getAuthorities(): Set<GrantedAuthority> =
      roles.map { SimpleGrantedAuthority("ROLE_" + it.name) }.toSet()

  override fun getPassword(): String? = password

  override fun getUsername(): String? = username

  override fun isEnabled(): Boolean = enabled ?: false

  fun resetVerification() {
    unverifiedMailAddress = null
    verificationCode = null
    verificationErrors = 0
    verificationExpiresAt = null
  }

  fun isMailAddressVerificationActive(): Boolean {
    return unverifiedMailAddress != null &&
        verificationErrors!! < VERIFICATION_MAX_ERRORS &&
        Instant.now() < verificationExpiresAt!!
  }

  fun isPasswordResetVerificationActive(): Boolean {
    return unverifiedMailAddress == null &&
        verificationErrors!! < VERIFICATION_MAX_ERRORS &&
        Instant.now() < verificationExpiresAt!!
  }

  fun clearForSoftDelete() {
    username = null
    password = null
    mailAddress = null
    unverifiedMailAddress = null
    givenName = null
    surname = null
    passwordChangedAt = null
    announcementsReadAt = null
    settings.clear()
    resetVerification()
  }

  fun displayName() =
      if (!givenName.isNullOrEmpty() && !surname.isNullOrEmpty()) "$givenName $surname" else null
}
