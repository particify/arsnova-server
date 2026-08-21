/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user.internal

import java.util.UUID
import org.springframework.ldap.core.DirContextOperations
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.ldap.userdetails.LdapUserDetailsMapper
import org.springframework.security.ldap.userdetails.UserDetailsContextMapper
import org.springframework.stereotype.Component

/**
 * Produces a mapper per LDAP registration. One per registration is required because
 * [UserDetailsContextMapper.mapUserFromContext] does not carry the provider ID.
 */
@Component
class LdapUserDetailsContextMapperFactory(
    private val provisioningService: LdapUserProvisioningService
) {
  fun create(providerId: UUID): UserDetailsContextMapper = ProvisioningMapper(providerId)

  private inner class ProvisioningMapper(private val providerId: UUID) : LdapUserDetailsMapper() {
    override fun mapUserFromContext(
        ctx: DirContextOperations,
        username: String,
        authorities: Collection<GrantedAuthority>
    ): UserDetails = provisioningService.provisionUser(providerId, ctx, username)
  }
}
