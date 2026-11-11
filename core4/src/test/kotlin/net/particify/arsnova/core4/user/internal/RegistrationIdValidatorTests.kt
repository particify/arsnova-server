/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user.internal

import java.util.UUID
import net.particify.arsnova.core4.user.sso.ExtendedSaml2RelyingPartyProperties
import net.particify.arsnova.core4.user.sso.ExtendedSaml2RelyingPartyProperties.ExtendedRegistration
import net.particify.arsnova.core4.user.sso.LdapProperties
import net.particify.arsnova.core4.user.sso.OidcProperties
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

private val LDAP_ID = UUID.fromString("1c5e7a39-4b62-4d8f-9a17-3e5c7b9d1f26")
private val SAML_ID = UUID.fromString("2d6f8b4a-5c73-4e9a-8b28-4f6d8c0e2a37")
private val OIDC_ID = UUID.fromString("3e7a9c5b-6d84-4fab-9c39-5a7e9d1f3b48")

class RegistrationIdValidatorTests {
  @Test
  fun shouldAcceptDistinctIds() {
    assertDoesNotThrow { validate(LDAP_ID, SAML_ID, OIDC_ID) }
  }

  @Test
  fun shouldRejectIdSharedBySamlAndOidc() {
    val message = rejectionMessage(LDAP_ID, SAML_ID, SAML_ID)
    Assertions.assertTrue(
        message.contains("$SAML_ID is configured for more than one protocol: OIDC, SAML"), message)
  }

  @Test
  fun shouldRejectIdSharedByLdapAndSaml() {
    val message = rejectionMessage(LDAP_ID, LDAP_ID, OIDC_ID)
    Assertions.assertTrue(
        message.contains("$LDAP_ID is configured for more than one protocol: LDAP, SAML"), message)
  }

  @Test
  fun shouldNameEveryProtocolSharingAnId() {
    val message = rejectionMessage(LDAP_ID, LDAP_ID, LDAP_ID)
    Assertions.assertTrue(
        message.contains("$LDAP_ID is configured for more than one protocol: LDAP, OIDC, SAML"))
  }

  private fun rejectionMessage(ldapId: UUID, samlId: UUID, oidcId: UUID): String =
      assertThrows<IllegalArgumentException> { validate(ldapId, samlId, oidcId) }.message!!

  private fun validate(ldapId: UUID, samlId: UUID, oidcId: UUID) =
      RegistrationIdValidator(
          LdapProperties(
              mapOf(
                  ldapId to
                      LdapProperties.Registration(
                          url = "ldap://ldap.example.com/dc=example,dc=com",
                          userDnPattern = "uid={0},ou=people"))),
          ExtendedSaml2RelyingPartyProperties(mapOf(samlId to ExtendedRegistration())),
          OidcProperties(
              mapOf(
                  oidcId to
                      OidcProperties.Registration(
                          issuerUri = "https://idp.example.com", clientId = "client"))))
}
