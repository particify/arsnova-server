/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.security

import java.util.UUID
import net.particify.arsnova.core4.system.migration.v3.MigrationProperties
import net.particify.arsnova.core4.user.internal.LdapProperties
import net.particify.arsnova.core4.user.internal.LdapProperties.Registration
import net.particify.arsnova.core4.user.internal.LdapUserDetailsContextMapperFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.ldap.core.support.BaseLdapPathContextSource
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher
import org.springframework.security.authentication.ProviderManager
import org.springframework.security.ldap.DefaultSpringSecurityContextSource
import org.springframework.security.ldap.authentication.BindAuthenticator
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider
import org.springframework.security.ldap.authentication.LdapAuthenticator
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch
import org.springframework.stereotype.Component

private const val CONNECT_TIMEOUT_PROPERTY = "com.sun.jndi.ldap.connect.timeout"
private const val READ_TIMEOUT_PROPERTY = "com.sun.jndi.ldap.read.timeout"

/**
 * Holds one [AuthenticationManager] per LDAP registration. Each of them contains a single provider
 * so that credentials submitted for one registration are never passed to another one: a
 * [ProviderManager] falls through to the next provider on an `AuthenticationException` and
 * [LdapAuthenticationProvider] claims any `UsernamePasswordAuthenticationToken`. For the same
 * reason the providers are constructed here instead of being exposed as beans.
 */
@Component
class LdapAuthenticationProviderRegistry(
    ldapProperties: LdapProperties,
    migrationProperties: MigrationProperties,
    private val mapperFactory: LdapUserDetailsContextMapperFactory,
    private val eventPublisher: ApplicationEventPublisher
) {
  companion object {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)
  }

  private val authenticationManagers: Map<UUID, AuthenticationManager>

  init {
    require(!migrationProperties.enabled || ldapProperties.registration.size <= 1) {
      "Only a single LDAP registration is supported while the v3 migration is enabled because " +
          "persistence.v3-migration.authentication-provider-mapping maps the v3 provider name " +
          "\"LDAP\" to exactly one provider ID. Migrated users would end up in whichever " +
          "registration matches that mapping. Configured registrations: " +
          "${ldapProperties.registration.keys}."
    }
    authenticationManagers =
        ldapProperties.registration.mapValues { createAuthenticationManager(it.key, it.value) }
    if (authenticationManagers.isNotEmpty()) {
      logger.debug("LDAP registrations: {}", authenticationManagers.keys)
    }
  }

  fun findByProviderId(providerId: UUID): AuthenticationManager? =
      authenticationManagers[providerId]

  private fun createAuthenticationManager(
      providerId: UUID,
      registration: Registration
  ): AuthenticationManager {
    val contextSource = createContextSource(registration)
    val provider = LdapAuthenticationProvider(createAuthenticator(registration, contextSource))
    provider.setUserDetailsContextMapper(mapperFactory.create(providerId))
    val providerManager = ProviderManager(provider)
    providerManager.setAuthenticationEventPublisher(
        DefaultAuthenticationEventPublisher(eventPublisher))
    return providerManager
  }

  private fun createContextSource(registration: Registration): BaseLdapPathContextSource {
    val contextSource = DefaultSpringSecurityContextSource(registration.url)
    // Spring LDAP has no typed setters for the timeouts. Without the read timeout, an
    // unresponsive directory keeps a request thread blocked indefinitely.
    contextSource.setBaseEnvironmentProperties(
        mapOf<String, Any>(
            CONNECT_TIMEOUT_PROPERTY to registration.connectTimeout.toMillis().toString(),
            READ_TIMEOUT_PROPERTY to registration.readTimeout.toMillis().toString()))
    // Manager credentials are needed for the search path if the directory does not allow
    // anonymous binds.
    val managerUserDn = registration.managerUserDn
    val managerPassword = registration.managerPassword
    if (!managerUserDn.isNullOrEmpty() && !managerPassword.isNullOrEmpty()) {
      contextSource.setUserDn(managerUserDn)
      contextSource.setPassword(managerPassword)
    }
    contextSource.afterPropertiesSet()
    return contextSource
  }

  /** Misconfigured registrations fail on startup instead of on the first login attempt. */
  private fun createAuthenticator(
      registration: Registration,
      contextSource: BaseLdapPathContextSource
  ): LdapAuthenticator {
    val authenticator = BindAuthenticator(contextSource)
    val requestedAttributes = registration.requestedAttributes.toTypedArray()
    authenticator.setUserAttributes(requestedAttributes)
    val searchFilter = registration.userSearchFilter
    if (searchFilter.isNullOrEmpty()) {
      authenticator.setUserDnPatterns(arrayOf(checkNotNull(registration.userDnPattern)))
    } else {
      val userSearch =
          FilterBasedLdapUserSearch(registration.userSearchBase, searchFilter, contextSource)
      // BindAuthenticator passes the attributes returned by the search on to the mapper instead
      // of reading the ones set above, so both paths need the same list.
      userSearch.setReturningAttributes(requestedAttributes)
      registration.searchTimeLimit?.let { userSearch.setSearchTimeLimit(it.toMillis().toInt()) }
      authenticator.setUserSearch(userSearch)
    }
    authenticator.afterPropertiesSet()
    return authenticator
  }
}
