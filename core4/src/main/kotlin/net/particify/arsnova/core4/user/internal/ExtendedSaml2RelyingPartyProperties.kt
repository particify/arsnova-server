/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user.internal

import java.util.UUID
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.security.saml2.autoconfigure.Saml2RelyingPartyProperties

private const val ID_ATTRIBUTE = "urn:oasis:names:tc:SAML:attribute:subject-id"
private const val MAIL_ATTRIBUTE = "urn:oid:0.9.2342.19200300.100.1.3"
private const val GIVEN_NAME_ATTRIBUTE = "urn:oid:2.5.4.42"
private const val SURNAME_ATTRIBUTE = "urn:oid:2.5.4.4"

@ConfigurationProperties("security.saml2.relyingparty")
data class ExtendedSaml2RelyingPartyProperties(
    val registration: Map<UUID, ExtendedRegistration> = mapOf()
) {
  class ExtendedRegistration : Saml2RelyingPartyProperties.Registration() {
    val attributeMapping = AttributeMapping()

    data class AttributeMapping(
        var id: String = ID_ATTRIBUTE,
        var mailAddress: String = MAIL_ATTRIBUTE,
        var givenName: String = GIVEN_NAME_ATTRIBUTE,
        var surname: String = SURNAME_ATTRIBUTE
    )
  }
}
