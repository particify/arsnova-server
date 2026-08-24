/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user

import com.unboundid.ldap.listener.InMemoryDirectoryServer
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig
import com.unboundid.ldap.listener.InMemoryListenerConfig
import com.unboundid.ldif.LDIFReader
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.core.io.ClassPathResource
import org.springframework.test.context.DynamicPropertyRegistrar

const val LDAP_BASE_DN = "dc=example,dc=com"
const val LDAP_PROVIDER_ID = "e2b1c33e-1d69-4b0b-9a08-25cbf6b8b8a1"
const val LOGIN_ATTEMPT_LIMIT = 3

/** Deliberately absent from the directory. */
const val UNKNOWN_USER_ID = "no-such-directory-user"

/** Deliberately absent from the directory as well: the throttle trips before the bind. */
const val THROTTLED_USER_ID = "throttled-user"

/** Account from the `dev` Liquibase fixtures, so it is absent from the directory. */
const val DEV_ACCOUNT_USERNAME = "user@example.com"
const val DEV_ACCOUNT_PASSWORD = "user"

val IMPORT_USER =
    LdapTestUser(
        userId = "mwendland",
        password = "not-a-real-password",
        givenName = "Marit",
        surname = "Wendland",
        mailAddress = "marit.wendland@example.com")
val CANONICAL_ID_USER = LdapTestUser(userId = "tilda.brekke", password = "dev-fixture-secret")
val REPEAT_LOGIN_USER = LdapTestUser(userId = "jorunn", password = "plaintext-on-purpose")
val HTTP_LOGIN_USER = LdapTestUser(userId = "dsalvatierra", password = "bind-me-please")
val WRONG_PASSWORD_USER = LdapTestUser(userId = "bkuiper", password = "never-submitted")
val LOCAL_PROVIDER_REJECTION_USER =
    LdapTestUser(userId = "haruka.oyelaran", password = "just-for-tests")

/** Its mail address is the one the dev fixtures' account already uses. */
val MAIL_COLLISION_USER = LdapTestUser(userId = "nikko.pham", password = "throwaway-credential")

/** Its user ID is the username the dev fixtures' account already uses. */
val USERNAME_COLLISION_USER =
    LdapTestUser(userId = "admin@example.com", password = "fixture-password-only")

val LDAP_TEST_USERS =
    listOf(
        IMPORT_USER,
        CANONICAL_ID_USER,
        REPEAT_LOGIN_USER,
        HTTP_LOGIN_USER,
        WRONG_PASSWORD_USER,
        LOCAL_PROVIDER_REJECTION_USER,
        MAIL_COLLISION_USER,
        USERNAME_COLLISION_USER)

/**
 * User IDs which tests expect to be rejected. An entry for any of them would invalidate those
 * tests.
 */
val ABSENT_USER_IDS = listOf(UNKNOWN_USER_ID, THROTTLED_USER_ID, DEV_ACCOUNT_USERNAME)

/** A user from `ldap/test.ldif`. Each one backs a single test to keep the tests independent. */
data class LdapTestUser(
    val userId: String,
    val password: String,
    val givenName: String? = null,
    val surname: String? = null,
    val mailAddress: String? = null
)

private const val EPHEMERAL_PORT = 0
private const val LDIF_RESOURCE = "ldap/test.ldif"

/**
 * Runs an in-process directory for the LDAP tests. Boot's `EmbeddedLdapAutoConfiguration` is not
 * used because it requires `spring-boot-ldap`, which also activates the single-`ContextSource`
 * `LdapAutoConfiguration`, and because it models exactly one directory.
 */
@TestConfiguration(proxyBeanMethods = false)
class LdapTestConfiguration {
  @Bean(destroyMethod = "close")
  fun inMemoryDirectoryServer(): InMemoryDirectoryServer {
    val config = InMemoryDirectoryServerConfig(LDAP_BASE_DN)
    config.setEnforceSingleStructuralObjectClass(false)
    config.setListenerConfigs(InMemoryListenerConfig.createLDAPConfig("LDAP", EPHEMERAL_PORT))
    val directoryServer = InMemoryDirectoryServer(config)
    // The base entry is part of the LDIF, so adding one here would fail as a duplicate.
    ClassPathResource(LDIF_RESOURCE).inputStream.use {
      directoryServer.importFromLDIF(true, LDIFReader(it))
    }
    directoryServer.startListening()
    return directoryServer
  }

  /** The port is only known once the server listens, so it is resolved when properties are read. */
  @Bean
  fun ldapPropertyRegistrar(directoryServer: InMemoryDirectoryServer) =
      DynamicPropertyRegistrar { registry ->
        val prefix = "security.ldap.registration.$LDAP_PROVIDER_ID"
        registry.add("$prefix.url") {
          "ldap://127.0.0.1:${directoryServer.listenPort}/$LDAP_BASE_DN"
        }
        registry.add("$prefix.user-dn-pattern") { "uid={0},ou=people" }
        registry.add("$prefix.imported-attributes") { "mail,givenName,sn" }
      }
}
