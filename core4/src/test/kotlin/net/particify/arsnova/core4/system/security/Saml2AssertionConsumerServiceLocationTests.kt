/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.security

import java.util.UUID
import net.particify.arsnova.core4.user.internal.ExtendedSaml2RelyingPartyProperties
import net.particify.arsnova.core4.user.internal.ExtendedSaml2RelyingPartyProperties.ExtendedRegistration
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration

private const val PRODUCT_NAME = "arsnova"

private const val BASE_URL = "http://localhost/api"
private const val PRIMARY_LOCATION = "{baseUrl}/auth/callback/saml"

private val REGISTRATION_ID = UUID.fromString("35c3a3c1-6a8e-4c0f-9d3a-1f5b2e8d4c70")
private val OTHER_REGISTRATION_ID = UUID.fromString("7f0e9c24-8b1d-4a5e-9c36-2d7a4e1b8f90")

private val RESOLVED_PRIMARY = "$BASE_URL/auth/callback/saml"
private val RESOLVED_DEFAULT = "$BASE_URL/login/saml2/sso/$REGISTRATION_ID"

private val ASSERTION_CONSUMER_SERVICE_PATTERN = Regex("<[\\w:]*AssertionConsumerService\\s[^>]*>")
private val XML_ATTRIBUTE_PATTERN = Regex("([\\w:]+)=\"([^\"]*)\"")

/** Which locations a registration accepts assertions at, and how the metadata advertises them. */
class Saml2AssertionConsumerServiceLocationTests {
  /** Nothing is configured, so there is nothing to accept beyond what Spring already does. */
  @Test
  fun shouldAcceptDefaultLocationWithoutConfiguration() {
    val accepted =
        Saml2AssertionConsumerServices(properties(DEFAULT_ASSERTION_CONSUMER_SERVICE_LOCATION))
            .acceptedLocations(registration(RESOLVED_DEFAULT))
    Assertions.assertEquals(listOf(RESOLVED_DEFAULT), accepted)
  }

  /**
   * The base URL is recovered from the primary location, which is the only resolved one available:
   * neither the validators nor the metadata customizer is handed the request.
   */
  @Test
  fun shouldAcceptConfiguredAndDefaultLocationWithPrimaryFirst() {
    val accepted =
        Saml2AssertionConsumerServices(properties(PRIMARY_LOCATION))
            .acceptedLocations(registration())
    Assertions.assertEquals(listOf(RESOLVED_PRIMARY, RESOLVED_DEFAULT), accepted)
  }

  /** The aggregate metadata endpoint resolves registrations no configuration here describes. */
  @Test
  fun shouldAcceptOnlyPrimaryLocationForUnknownRegistration() {
    val accepted =
        Saml2AssertionConsumerServices(properties(PRIMARY_LOCATION))
            .acceptedLocations(registration(registrationId = OTHER_REGISTRATION_ID))
    Assertions.assertEquals(listOf(RESOLVED_PRIMARY), accepted)
  }

  /**
   * A location resolved by something this does not model leaves the base URL unknown, and guessing
   * it would widen what is accepted on no evidence.
   */
  @Test
  fun shouldAcceptOnlyPrimaryLocationWhenBaseUrlCannotBeRecovered() {
    val accepted =
        Saml2AssertionConsumerServices(properties(PRIMARY_LOCATION))
            .acceptedLocations(registration("$BASE_URL/elsewhere"))
    Assertions.assertEquals(listOf("$BASE_URL/elsewhere"), accepted)
  }

  /** What an identity provider reads to learn both endpoints are live while a move is under way. */
  @Test
  fun shouldAdvertiseEveryAcceptedLocation() {
    val services = assertionConsumerServices(resolve())
    Assertions.assertEquals(
        listOf(RESOLVED_PRIMARY, RESOLVED_DEFAULT), services.map { it["Location"] })
  }

  /** An index identifies a service, so two services carrying the same one is invalid metadata. */
  @Test
  fun shouldAdvertiseDistinctIndexPerAssertionConsumerService() {
    val indexes = assertionConsumerServices(resolve()).map { it["index"] }
    Assertions.assertEquals(indexes.size, indexes.toSet().size, indexes.toString())
  }

  /** Document order decides otherwise, which is not something this controls. */
  @Test
  fun shouldMarkOnlyPrimaryAssertionConsumerServiceAsDefault() {
    val services = assertionConsumerServices(resolve())
    Assertions.assertEquals(
        listOf("true", null), services.map { it["isDefault"] }, services.toString())
  }

  /** Whatever the primary is bound to is what the default one is reachable with too. */
  @Test
  fun shouldAdvertiseDefaultLocationWithPrimaryBinding() {
    val bindings = assertionConsumerServices(resolve()).map { it["Binding"] }
    Assertions.assertEquals(1, bindings.toSet().size, bindings.toString())
  }

  private fun resolve(): String =
      Saml2SpMetadataFactory(PRODUCT_NAME, properties(PRIMARY_LOCATION))
          .metadataResolver()
          .resolve(registration())

  private fun assertionConsumerServices(metadata: String): List<Map<String, String>> =
      ASSERTION_CONSUMER_SERVICE_PATTERN.findAll(metadata)
          .map { element ->
            XML_ATTRIBUTE_PATTERN.findAll(element.value).associate {
              it.groupValues[1] to it.groupValues[2]
            }
          }
          .toList()

  private fun properties(location: String): ExtendedSaml2RelyingPartyProperties {
    val registration =
        ExtendedRegistration().apply {
          entityId = "https://example.com/saml/$REGISTRATION_ID"
          acs.location = location
        }
    return ExtendedSaml2RelyingPartyProperties(mapOf(REGISTRATION_ID to registration))
  }

  private fun registration(
      location: String = RESOLVED_PRIMARY,
      registrationId: UUID = REGISTRATION_ID
  ): RelyingPartyRegistration =
      RelyingPartyRegistration.withRegistrationId(registrationId.toString())
          .entityId("https://example.com/saml/$registrationId")
          .assertionConsumerServiceLocation(location)
          .assertingPartyMetadata {
            it.entityId("https://idp.example.com/saml")
                .singleSignOnServiceLocation("https://idp.example.com/saml/sso")
          }
          .build()
}
