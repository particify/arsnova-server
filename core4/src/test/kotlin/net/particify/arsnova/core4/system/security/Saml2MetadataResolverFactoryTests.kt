/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.security

import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import net.particify.arsnova.core4.user.SAML_IDP_ENTITY_ID
import net.particify.arsnova.core4.user.Saml2TestIdentityProvider
import net.particify.arsnova.core4.user.relyingPartyProperties
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir

private const val AGGREGATE_REGISTRATION_ID = "a3b4c5d6-7e8f-4901-a2b3-c4d5e6f70819"
private const val FIRST_ENTITY_ID = "https://first.idp.example.com/saml2"
private const val SECOND_ENTITY_ID = "https://second.idp.example.com/saml2"

/**
 * A federation aggregate describes more than one identity provider, and binding to whichever one
 * comes first would be a misconfiguration nobody notices. The error which says so is what an
 * operator gets to work with, so it is asserted literally.
 */
class Saml2MetadataResolverFactoryTests {
  private val registrationId = UUID.fromString(AGGREGATE_REGISTRATION_ID)
  private val identityProvider = Saml2TestIdentityProvider()
  private var repository: RefreshableRelyingPartyRegistrationRepository? = null
  private lateinit var aggregateLocation: String

  @BeforeEach
  fun writeAggregate(@TempDir directory: Path) {
    val path = directory.resolve("aggregate.xml")
    Files.writeString(path, aggregate(FIRST_ENTITY_ID, SECOND_ENTITY_ID))
    aggregateLocation = "file:$path"
  }

  @AfterEach
  fun releaseResources() {
    repository?.destroy()
  }

  @Test
  fun shouldRejectAggregateDescribingSeveralIdentityProviders() {
    val exception = assertThrows<IllegalArgumentException> { createRepository() }
    val message = checkNotNull(exception.message)
    Assertions.assertTrue(message.contains("2 identity providers"), message)
    Assertions.assertTrue(message.contains(FIRST_ENTITY_ID), message)
    Assertions.assertTrue(message.contains(SECOND_ENTITY_ID), message)
  }

  private fun createRepository(): RefreshableRelyingPartyRegistrationRepository =
      RefreshableRelyingPartyRegistrationRepository(
              relyingPartyProperties(registrationId, identityProvider, aggregateLocation))
          .also { repository = it }

  private fun aggregate(vararg entityIds: String): String {
    val entities =
        entityIds.joinToString("\n") {
          identityProvider.metadataDocument.replace(
              "entityID=\"$SAML_IDP_ENTITY_ID\"", "entityID=\"$it\"")
        }
    return "<md:EntitiesDescriptor xmlns:md=\"urn:oasis:names:tc:SAML:2.0:metadata\">\n" +
        entities +
        "\n</md:EntitiesDescriptor>"
  }
}
