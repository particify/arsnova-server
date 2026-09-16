/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user.internal

import java.util.UUID
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.security.saml2.autoconfigure.Saml2RelyingPartyProperties
import org.springframework.core.io.Resource

private const val DEFAULT_TITLE = "SAML"
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

    /**
     * Needs a setter, unlike [attributeMapping]: the inherited base class is bound as a JavaBean,
     * and for a getter-only scalar that binding fails for every value except the default.
     */
    var usernameMapping = UsernameMapping.MAIL_ADDRESS

    /**
     * Name of the `ExternalLoginLinkingStrategy` which selects the account a new login from this
     * registration is attached to. Unset means that every new login creates an account of its own.
     * No strategy is available unless one is registered, and a name none of the registered ones
     * carries is rejected while the application starts.
     *
     * Needs a setter for the same reason as [usernameMapping].
     */
    var linkingStrategy: String? = null

    /**
     * Name shown to users selecting this registration on the login page. Needs a setter for the
     * same reason as [usernameMapping].
     */
    var title = DEFAULT_TITLE

    /**
     * Position of this registration among the login options. Needs a setter for the same reason as
     * [usernameMapping].
     */
    var order = 0

    /**
     * Certificates the identity provider metadata's own signature is verified against. Getter-only
     * like [attributeMapping], because the binder mutates the list it hands back.
     *
     * Deliberately not Boot's `assertingparty.verification.credentials`, which pins the
     * certificates that verify *assertions*. A federation signs the aggregate it publishes with the
     * operator's own key rather than with any member identity provider's, and the two rotate on
     * unrelated cadences.
     *
     * It sits beside `assertingparty` rather than under it because Boot's `getAssertingparty()` is
     * getter-only and the type it returns cannot be extended.
     */
    val metadataVerification = MetadataVerification()

    data class AttributeMapping(
        var id: String = ID_ATTRIBUTE,
        var mailAddress: String = MAIL_ATTRIBUTE,
        var givenName: String = GIVEN_NAME_ATTRIBUTE,
        var surname: String = SURNAME_ATTRIBUTE
    )

    /**
     * Unset -- the default -- accepts the metadata document as read, which is all a location whose
     * transport is already trusted needs.
     *
     * A signature matching any one of the certificates is accepted, and that is what carries a
     * rotation: during the federation's announced overlap window both the outgoing and the incoming
     * certificate are configured, and the outgoing one is removed once it has passed.
     */
    class MetadataVerification {
      val credentials: MutableList<Credential> = mutableListOf()

      class Credential {
        var certificateLocation: Resource? = null
      }
    }
  }
}
