/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.security

import java.nio.file.Files
import java.nio.file.Path
import java.security.cert.X509Certificate
import java.util.UUID
import net.particify.arsnova.core4.user.SAML_IDP_ENTITY_ID
import net.particify.arsnova.core4.user.Saml2TestIdentityProvider
import net.particify.arsnova.core4.user.relyingPartyProperties
import net.shibboleth.shared.component.ComponentInitializationException
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
 * Covers the two things settled while a resolver is built: which identity provider a registration
 * binds to, and whether the metadata document's signature is checked before it is believed.
 *
 * A federation aggregate describes more than one identity provider, and binding to whichever one
 * comes first would be a misconfiguration nobody notices. The error which says so is what an
 * operator gets to work with, so it is asserted literally.
 */
class Saml2MetadataResolverFactoryTests {
  private val registrationId = UUID.fromString(AGGREGATE_REGISTRATION_ID)
  private val identityProvider = Saml2TestIdentityProvider()
  private val federationCredential = identityProvider.generateCredential()
  private var repository: RefreshableRelyingPartyRegistrationRepository? = null
  private lateinit var directory: Path

  @BeforeEach
  fun useTemporaryDirectory(@TempDir directory: Path) {
    this.directory = directory
  }

  @AfterEach
  fun releaseResources() {
    repository?.destroy()
  }

  @Test
  fun shouldRejectAggregateWithoutConfiguredEntityId() {
    val location = write("aggregate.xml", aggregate(FIRST_ENTITY_ID, SECOND_ENTITY_ID))
    val exception = assertThrows<IllegalArgumentException> { createRepository(location) }
    val message = checkNotNull(exception.message)
    Assertions.assertTrue(message.contains(FIRST_ENTITY_ID), message)
    Assertions.assertTrue(message.contains(SECOND_ENTITY_ID), message)
    Assertions.assertTrue(message.contains("assertingparty.entity-id"), message)
  }

  @Test
  fun shouldSelectConfiguredEntityFromAggregate() {
    val location = write("aggregate.xml", aggregate(FIRST_ENTITY_ID, SECOND_ENTITY_ID))
    val repository = createRepository(location, assertingPartyEntityId = SECOND_ENTITY_ID)
    val registration = checkNotNull(repository.findByRegistrationId(registrationId.toString()))
    Assertions.assertEquals(SECOND_ENTITY_ID, registration.assertingPartyMetadata.entityId)
  }

  /** Nothing is pinned, which is the default and has to keep working. */
  @Test
  fun shouldResolveUnsignedMetadataWithoutPinnedCertificate() {
    val repository = createRepository(write("idp.xml", identityProvider.metadataDocument))
    Assertions.assertNotNull(repository.findByRegistrationId(registrationId.toString()))
  }

  @Test
  fun shouldResolveMetadataSignedWithPinnedCertificate() {
    val repository = createRepository(signedMetadata(), certificates = listOf(published()))
    Assertions.assertNotNull(repository.findByRegistrationId(registrationId.toString()))
  }

  @Test
  fun shouldFailStartupWhenMetadataIsSignedWithUnpinnedCertificate() {
    val foreign = identityProvider.generateCredential().entityCertificate
    assertThrows<ComponentInitializationException> {
      createRepository(signedMetadata(), certificates = listOf(foreign))
    }
  }

  /**
   * The rotation-overlap case, and the reason the property is a list: during the window a
   * federation announces, the outgoing and the incoming certificate are both configured and a
   * signature matching either one is accepted.
   */
  @Test
  fun shouldResolveMetadataMatchingAnyPinnedCertificate() {
    val outgoing = identityProvider.generateCredential().entityCertificate
    val repository =
        createRepository(signedMetadata(), certificates = listOf(outgoing, published()))
    Assertions.assertNotNull(repository.findByRegistrationId(registrationId.toString()))
  }

  private fun createRepository(
      metadataUri: String,
      assertingPartyEntityId: String? = null,
      certificates: List<X509Certificate> = emptyList()
  ): RefreshableRelyingPartyRegistrationRepository =
      RefreshableRelyingPartyRegistrationRepository(
              relyingPartyProperties(
                  registrationId,
                  identityProvider,
                  metadataUri,
                  assertingPartyEntityId,
                  certificates))
          .also { repository = it }

  /** The certificate the document is actually signed with. */
  private fun published(): X509Certificate = federationCredential.entityCertificate

  private fun signedMetadata(): String =
      write(
          "signed.xml",
          identityProvider.signMetadata(identityProvider.metadataDocument, federationCredential))

  private fun write(fileName: String, document: String): String {
    val path = directory.resolve(fileName)
    Files.writeString(path, document)
    return "file:$path"
  }

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
