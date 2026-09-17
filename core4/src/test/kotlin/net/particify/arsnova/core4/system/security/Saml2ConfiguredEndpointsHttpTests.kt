/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.security

import java.util.Base64
import java.util.UUID
import net.particify.arsnova.core4.TestcontainersConfiguration
import net.particify.arsnova.core4.user.Saml2TestIdentityProvider
import net.particify.arsnova.core4.user.Saml2TestUser
import net.particify.arsnova.core4.user.acsLocation
import net.particify.arsnova.core4.user.registerRelyingParty
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistrar
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post

private const val REGISTRATION_ID = "a2b7c4d9-3e51-4f68-9a02-5c8e7b1d3f42"

/**
 * Where an ARSnova 3 installation received its assertions, used here because that is the case this
 * configuration exists for. Nothing outside this file knows it.
 */
private const val CONFIGURED_ACS_PATH = "/auth/callback/saml"

private const val DEFAULT_ACS_PATH = "/login/saml2/sso/$REGISTRATION_ID"
private const val DEFAULT_METADATA_PATH = "/saml2/service-provider-metadata/$REGISTRATION_ID"

/** The payload shapes which fail before an authentication manager is reached. */
private const val ADDRESSED_ELSEWHERE = "addressed elsewhere"
private const val WITHOUT_ISSUER = "without an issuer"
private const val WITH_EMPTY_ISSUER = "with an empty issuer"
private const val NOT_WELL_FORMED = "not well-formed"
private const val NOT_A_RESPONSE = "not a response"

private const val PROTOCOL_NAMESPACE = "urn:oasis:names:tc:SAML:2.0:protocol"

/** The response's own issuer, which precedes the assertion's in document order. */
private val RESPONSE_ISSUER_PATTERN =
    Regex("<[\\w:]*Issuer[\\s>].*?</[\\w:]*Issuer>", RegexOption.DOT_MATCHES_ALL)

private val USER =
    Saml2TestUser(
        subjectId = "saml-configured-subject", mailAddress = "saml.configured@example.com")

/**
 * A single registration, and its own identity provider so the other SAML tests cannot reach it. A
 * configured assertion consumer service carries no registration ID, so a response arriving there is
 * resolved by the asserting party's entity ID -- which every identity provider in these tests
 * shares, and which only identifies a registration while the context describes one.
 */
@TestConfiguration(proxyBeanMethods = false)
class Saml2ConfiguredEndpointsTestConfiguration {
  @Bean fun configuredEndpointsIdentityProvider() = Saml2TestIdentityProvider()

  @Bean
  fun configuredEndpointsPropertyRegistrar(identityProvider: Saml2TestIdentityProvider) =
      DynamicPropertyRegistrar { registry ->
        registerRelyingParty(
            registry,
            identityProvider,
            REGISTRATION_ID,
            acsLocation = "{baseUrl}$CONFIGURED_ACS_PATH")
      }
}

/**
 * A registration serving an assertion consumer service of its own, which is what lets an identity
 * provider configured against an older deployment go on working while it has not read the current
 * metadata.
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration::class, Saml2ConfiguredEndpointsTestConfiguration::class)
class Saml2ConfiguredEndpointsHttpTests {
  @Autowired lateinit var mockMvc: MockMvc
  @Autowired lateinit var identityProvider: Saml2TestIdentityProvider

  private val registrationId = UUID.fromString(REGISTRATION_ID)

  @Test
  fun shouldAuthenticateAtConfiguredAssertionConsumerService() {
    Assertions.assertEquals(HttpStatus.OK.value(), login(CONFIGURED_ACS_PATH))
  }

  /** Accepted with no configuration asking for it, so a move away never has to add it back. */
  @Test
  fun shouldAuthenticateAtDefaultAssertionConsumerService() {
    Assertions.assertEquals(HttpStatus.OK.value(), login(DEFAULT_ACS_PATH))
  }

  /**
   * Widened, not switched off: a location the registration does not carry is still refused, and the
   * two endpoints refuse it the same way.
   *
   * The configured one carries no registration ID, so its converter always resolves through the
   * asserting party's entity ID. That branch reports what it cannot read by throwing something
   * which is not an `AuthenticationException`, which no failure handler would answer, so a payload
   * failing there has to be checked against the endpoint which does not take that branch.
   */
  @ParameterizedTest
  @ValueSource(
      strings =
          [ADDRESSED_ELSEWHERE, WITHOUT_ISSUER, WITH_EMPTY_ISSUER, NOT_WELL_FORMED, NOT_A_RESPONSE])
  fun shouldRejectAtBothAssertionConsumerServicesAlike(case: String) {
    val payload = rejectedResponse(case)
    val configured = submit(CONFIGURED_ACS_PATH, payload)
    val default = submit(DEFAULT_ACS_PATH, payload)
    Assertions.assertEquals(default, configured, case)
    Assertions.assertEquals(HttpStatus.FOUND.value(), default, case)
  }

  @Test
  fun shouldAdvertiseBothAssertionConsumerServices() {
    val metadata = metadata(DEFAULT_METADATA_PATH)
    for (path in listOf(CONFIGURED_ACS_PATH, DEFAULT_ACS_PATH)) {
      Assertions.assertTrue(
          metadata.contains("Location=\"${acsLocation(registrationId, path)}\""), metadata)
    }
  }

  private fun metadata(path: String): String {
    val response = mockMvc.perform(get(path)).andReturn().response
    Assertions.assertEquals(HttpStatus.OK.value(), response.status)
    return response.contentAsString
  }

  private fun login(path: String, destination: String = acsLocation(registrationId, path)): Int =
      submit(
          path, identityProvider.encodedResponse(registrationId, USER, acsLocation = destination))

  private fun submit(path: String, samlResponse: String): Int =
      mockMvc.perform(post(path).param("SAMLResponse", samlResponse)).andReturn().response.status

  private fun rejectedResponse(case: String): String =
      when (case) {
        ADDRESSED_ELSEWHERE -> response(acsLocation(registrationId, "/saml/unknown"))
        WITHOUT_ISSUER -> encode(RESPONSE_ISSUER_PATTERN.replaceFirst(decodedResponse(), ""))
        WITH_EMPTY_ISSUER ->
            encode(RESPONSE_ISSUER_PATTERN.replaceFirst(decodedResponse(), "<saml2:Issuer/>"))
        NOT_WELL_FORMED -> encode("This is not a SAML response.")
        else -> encode("<samlp:AuthnRequest xmlns:samlp=\"$PROTOCOL_NAMESPACE\" />")
      }

  private fun response(acsLocation: String): String =
      identityProvider.encodedResponse(registrationId, USER, acsLocation = acsLocation)

  private fun decodedResponse(): String =
      String(Base64.getDecoder().decode(response(acsLocation(registrationId, CONFIGURED_ACS_PATH))))

  private fun encode(payload: String): String =
      Base64.getEncoder().encodeToString(payload.toByteArray())
}
