/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user.internal

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import java.util.UUID
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.security.saml2.autoconfigure.Saml2RelyingPartyProperties
import org.springframework.core.io.Resource
import org.springframework.validation.annotation.Validated

private const val DEFAULT_TITLE = "SAML"
private const val ID_ATTRIBUTE = "urn:oasis:names:tc:SAML:attribute:subject-id"
private const val MAIL_ATTRIBUTE = "urn:oid:0.9.2342.19200300.100.1.3"
private const val GIVEN_NAME_ATTRIBUTE = "urn:oid:2.5.4.42"
private const val SURNAME_ATTRIBUTE = "urn:oid:2.5.4.4"

@ConfigurationProperties("security.saml2.relyingparty")
@Validated
data class ExtendedSaml2RelyingPartyProperties(
    @field:Valid val registration: Map<UUID, ExtendedRegistration> = mapOf()
) {
  class ExtendedRegistration : Saml2RelyingPartyProperties.Registration() {
    @field:Valid val attributeMapping = AttributeMapping()

    /**
     * Attributes published in the service provider metadata beyond those [attributeMapping] names,
     * for a federation which requires them to be requested even though nothing here reads them.
     * Getter-only like [attributeMapping], because the binder mutates the list it hands back.
     */
    @field:Valid
    val additionalRequestedAttributes: MutableList<RequestedAttribute> = mutableListOf()

    /**
     * A further path this registration's own metadata document is served at, besides
     * `/saml2/service-provider-metadata/{registrationId}`, which is always served and cannot be
     * turned off. A servlet path rather than a URL: nothing publishes it, so there is no
     * `{baseUrl}` to resolve.
     *
     * Not to be confused with `assertingparty.metadata-uri`, which is where the identity provider's
     * document is read from. Needs a setter for the same reason as [usernameMapping].
     */
    var metadataPath: String? = null

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

    /**
     * A list of objects rather than of names because `required` is what an identity provider keys
     * its release policy on, and it differs per attribute.
     */
    class RequestedAttribute {
      @field:NotBlank(
          message =
              "must name the attribute to request, which cannot be derived from anything else")
      var name: String = ""

      /** Whether a login is expected to fail without it, which is never the case here. */
      var required: Boolean = false
    }

    /**
     * Blank is rejected rather than read as "do not map this": an empty name would be looked up in
     * the assertion like any other and published as an empty requested attribute.
     */
    data class AttributeMapping(
        @field:NotBlank(message = "must name the asserted attribute holding the user ID")
        var id: String = ID_ATTRIBUTE,
        @field:NotBlank(message = "must name the asserted attribute holding the mail address")
        var mailAddress: String = MAIL_ATTRIBUTE,
        @field:NotBlank(message = "must name the asserted attribute holding the given name")
        var givenName: String = GIVEN_NAME_ATTRIBUTE,
        @field:NotBlank(message = "must name the asserted attribute holding the surname")
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
