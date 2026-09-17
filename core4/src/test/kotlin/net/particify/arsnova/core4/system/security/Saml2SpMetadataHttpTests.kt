/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.security

import net.particify.arsnova.core4.TestcontainersConfiguration
import net.particify.arsnova.core4.user.SAML_GIVEN_NAME_ATTRIBUTE
import net.particify.arsnova.core4.user.SAML_MAIL_ATTRIBUTE
import net.particify.arsnova.core4.user.SAML_MAIL_REGISTRATION_ID
import net.particify.arsnova.core4.user.SAML_SURNAME_ATTRIBUTE
import net.particify.arsnova.core4.user.Saml2TestConfiguration
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get

private const val METADATA_PATH = "/saml2/service-provider-metadata"

/**
 * The endpoint v4 publishes, which nothing else exercises. Configured exactly like
 * `Saml2LoginHttpTests` so both share one application context.
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration::class, Saml2TestConfiguration::class)
class Saml2SpMetadataHttpTests {
  @Autowired lateinit var mockMvc: MockMvc

  @Test
  fun shouldPublishRequestedAttributes() {
    val metadata = resolve()
    Assertions.assertTrue(metadata.contains("AttributeConsumingService"), metadata)
    for (attribute in
        listOf(SAML_MAIL_ATTRIBUTE, SAML_GIVEN_NAME_ATTRIBUTE, SAML_SURNAME_ATTRIBUTE)) {
      Assertions.assertTrue(metadata.contains("Name=\"$attribute\""), metadata)
    }
  }

  /** The registrations keep the default subject ID mapping, which is not an ordinary attribute. */
  @Test
  fun shouldPublishSubjectIdRequirement() {
    val metadata = resolve()
    Assertions.assertTrue(
        metadata.contains("urn:oasis:names:tc:SAML:profiles:subject-id:req"), metadata)
    Assertions.assertTrue(metadata.contains(">subject-id<"), metadata)
  }

  /** Spring publishes one only for a registration configuring `name-id-format`, as these do not. */
  @Test
  fun shouldNotPublishNameIdFormatWithoutConfiguration() {
    val metadata = resolve()
    Assertions.assertFalse(metadata.contains("NameIDFormat"), metadata)
  }

  /** A registration signs unless it opts out, and what it does is what the document says. */
  @Test
  fun shouldPublishAuthnRequestsSigned() {
    val metadata = resolve()
    Assertions.assertTrue(metadata.contains("AuthnRequestsSigned=\"true\""), metadata)
  }

  private fun resolve(): String {
    val response =
        mockMvc.perform(get("$METADATA_PATH/$SAML_MAIL_REGISTRATION_ID")).andReturn().response
    Assertions.assertEquals(HttpStatus.OK.value(), response.status)
    return response.contentAsString
  }
}
