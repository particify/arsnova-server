/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.compat

import net.particify.arsnova.core4.TestcontainersConfiguration
import net.particify.arsnova.core4.system.compat.LegacyConfigurationController.LegacyConfiguration
import net.particify.arsnova.core4.system.compat.LegacyConfigurationController.LegacyConfiguration.LegacyAuthenticationProvider
import net.particify.arsnova.core4.system.compat.LegacyConfigurationController.LegacyConfiguration.LegacyAuthenticationProvider.Role
import net.particify.arsnova.core4.user.LDAP_PROVIDER_ID
import net.particify.arsnova.core4.user.LdapTestConfiguration
import net.particify.arsnova.core4.user.Saml2TestIdentityProvider
import net.particify.arsnova.core4.user.registerRelyingParty
import net.particify.arsnova.core4.user.registerRelyingPartyDisplay
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistrar
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.json.JsonMapper

/** Registration which publishes a configured title and order. */
private const val TITLED_SAML_REGISTRATION_ID = "a7b3c2d1-4e5f-4a6b-8c9d-0e1f2a3b4c5d"

/** Registration which publishes the defaults, which no client may lose. */
private const val DEFAULT_SAML_REGISTRATION_ID = "d4c3b2a1-9f8e-4d7c-8b6a-5f4e3d2c1b0a"

private const val SAML_TITLE = "Example IdP"
private const val SAML_ORDER = 3
private const val DEFAULT_SAML_TITLE = "SAML"
private const val DEFAULT_LDAP_TITLE = "LDAP"
private const val PRODUCT_NAME = "Example ARS"
private const val PRODUCT_NAME_PROPERTY = "service.product-name=$PRODUCT_NAME"
private const val AUTO_ASSIGN_TO_PROPERTY = "security.room-creator-role.auto-assign-to"

private val CREATOR_ROLES = listOf(Role.MODERATOR, Role.PARTICIPANT)
private val NON_CREATOR_ROLES = listOf(Role.PARTICIPANT)

/**
 * Two relying parties, because the published title and order are per registration: without them a
 * deployment with two identity providers offers two indistinguishable login options.
 */
@TestConfiguration(proxyBeanMethods = false)
class LegacyConfigurationTestConfiguration {
  @Bean fun saml2TestIdentityProvider() = Saml2TestIdentityProvider()

  @Bean
  fun legacyConfigurationSaml2PropertyRegistrar(identityProvider: Saml2TestIdentityProvider) =
      DynamicPropertyRegistrar { registry ->
        registerRelyingParty(registry, identityProvider, TITLED_SAML_REGISTRATION_ID)
        registerRelyingPartyDisplay(registry, TITLED_SAML_REGISTRATION_ID, SAML_TITLE, SAML_ORDER)
        registerRelyingParty(registry, identityProvider, DEFAULT_SAML_REGISTRATION_ID)
      }
}

/** Shared access to the payload of the compatibility endpoint the v3 clients read. */
abstract class LegacyConfigurationHttpTestSupport {
  @Autowired lateinit var mockMvc: MockMvc
  @Autowired lateinit var jsonMapper: JsonMapper

  protected fun provider(id: String): LegacyAuthenticationProvider {
    val response =
        mockMvc
            .perform(get("/configuration"))
            .andExpect(status().isOk())
            .andReturn()
            .response
            .contentAsString
    val configuration = jsonMapper.readValue(response, LegacyConfiguration::class.java)
    return configuration.authenticationProviders.single { it.id == id }
  }

  protected fun assertAllowedRoles(expected: List<Role>, id: String) {
    Assertions.assertEquals(expected, provider(id).allowedRoles, "Roles of provider $id")
  }
}

@SpringBootTest(properties = [PRODUCT_NAME_PROPERTY, "$AUTO_ASSIGN_TO_PROPERTY=all-accounts"])
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(
    TestcontainersConfiguration::class,
    LdapTestConfiguration::class,
    LegacyConfigurationTestConfiguration::class)
class LegacyConfigurationAllAccountsHttpTests : LegacyConfigurationHttpTestSupport() {
  @Test
  fun shouldPublishCreatorRoleForEveryProvider() {
    assertAllowedRoles(CREATOR_ROLES, "user-db")
    assertAllowedRoles(CREATOR_ROLES, "guest")
    assertAllowedRoles(CREATOR_ROLES, LDAP_PROVIDER_ID)
    assertAllowedRoles(CREATOR_ROLES, TITLED_SAML_REGISTRATION_ID)
    assertAllowedRoles(CREATOR_ROLES, DEFAULT_SAML_REGISTRATION_ID)
  }

  @Test
  fun shouldPublishProductNameAsLocalProviderTitle() {
    val provider = provider("user-db")
    Assertions.assertEquals(PRODUCT_NAME, provider.title)
    Assertions.assertEquals(0, provider.order)
  }

  @Test
  fun shouldPublishConfiguredSaml2TitleAndOrder() {
    val provider = provider(TITLED_SAML_REGISTRATION_ID)
    Assertions.assertEquals(SAML_TITLE, provider.title)
    Assertions.assertEquals(SAML_ORDER, provider.order)
  }

  @Test
  fun shouldPublishDefaultSaml2TitleAndOrder() {
    val provider = provider(DEFAULT_SAML_REGISTRATION_ID)
    Assertions.assertEquals(DEFAULT_SAML_TITLE, provider.title)
    Assertions.assertEquals(0, provider.order)
  }

  @Test
  fun shouldPublishLdapTitle() {
    Assertions.assertEquals(DEFAULT_LDAP_TITLE, provider(LDAP_PROVIDER_ID).title)
  }
}

@SpringBootTest(properties = [PRODUCT_NAME_PROPERTY, "$AUTO_ASSIGN_TO_PROPERTY=verified-accounts"])
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(
    TestcontainersConfiguration::class,
    LdapTestConfiguration::class,
    LegacyConfigurationTestConfiguration::class)
class LegacyConfigurationVerifiedAccountsHttpTests : LegacyConfigurationHttpTestSupport() {
  @Test
  fun shouldDropCreatorRoleFromGuestProviderOnly() {
    assertAllowedRoles(NON_CREATOR_ROLES, "guest")
    assertAllowedRoles(CREATOR_ROLES, "user-db")
    assertAllowedRoles(CREATOR_ROLES, LDAP_PROVIDER_ID)
    assertAllowedRoles(CREATOR_ROLES, TITLED_SAML_REGISTRATION_ID)
    assertAllowedRoles(CREATOR_ROLES, DEFAULT_SAML_REGISTRATION_ID)
  }
}

@SpringBootTest(properties = [PRODUCT_NAME_PROPERTY, "$AUTO_ASSIGN_TO_PROPERTY=none"])
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(
    TestcontainersConfiguration::class,
    LdapTestConfiguration::class,
    LegacyConfigurationTestConfiguration::class)
class LegacyConfigurationNoAccountsHttpTests : LegacyConfigurationHttpTestSupport() {
  @Test
  fun shouldDropCreatorRoleFromEveryProvider() {
    assertAllowedRoles(NON_CREATOR_ROLES, "user-db")
    assertAllowedRoles(NON_CREATOR_ROLES, "guest")
    assertAllowedRoles(NON_CREATOR_ROLES, LDAP_PROVIDER_ID)
    assertAllowedRoles(NON_CREATOR_ROLES, TITLED_SAML_REGISTRATION_ID)
    assertAllowedRoles(NON_CREATOR_ROLES, DEFAULT_SAML_REGISTRATION_ID)
  }
}
