/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.security

import java.util.UUID
import net.particify.arsnova.core4.user.Saml2TestIdentityProvider
import net.particify.arsnova.core4.user.relyingPartyProperties
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

private val REGISTRATION_ID = UUID.fromString("35c3a3c1-6a8e-4c0f-9d3a-1f5b2e8d4c70")

/** Whether the registrations the repository assembles sign their authentication requests. */
class Saml2RequestSigningTests {
  private lateinit var identityProvider: Saml2TestIdentityProvider
  private var repository: RefreshableRelyingPartyRegistrationRepository? = null

  @BeforeEach
  fun startIdentityProvider() {
    identityProvider = Saml2TestIdentityProvider()
  }

  @AfterEach
  fun releaseResources() {
    repository?.destroy()
  }

  /** The published metadata declares signing, so nothing may leave a registration unsigned. */
  @Test
  fun shouldSignWithoutConfiguration() {
    Assertions.assertTrue(registration().isAuthnRequestsSigned)
  }

  /** The only way out, for an identity provider which rejects a signed request. */
  @Test
  fun shouldNotSignWhenOptedOut() {
    Assertions.assertFalse(registration(signRequest = false).isAuthnRequestsSigned)
  }

  /** An explicit `true` is what the property used to mean, and it still means it. */
  @Test
  fun shouldSignWhenAskedFor() {
    Assertions.assertTrue(registration(signRequest = true).isAuthnRequestsSigned)
  }

  /**
   * Without this the application starts fine and fails at the first login with `Failed to resolve
   * any signing credential`, naming neither the registration nor the property.
   */
  @Test
  fun shouldRejectRegistrationWithoutSigningCredential() {
    val properties = relyingPartyProperties(REGISTRATION_ID, identityProvider)
    properties.registration.getValue(REGISTRATION_ID).signing.credentials.clear()
    val exception =
        assertThrows<IllegalArgumentException> {
          RefreshableRelyingPartyRegistrationRepository(properties).also { repository = it }
        }
    for (expected in listOf("$REGISTRATION_ID", "signing.credentials")) {
      Assertions.assertTrue(exception.message!!.contains(expected), exception.message)
    }
  }

  private fun registration(signRequest: Boolean? = null) =
      checkNotNull(repository(signRequest).findByRegistrationId(REGISTRATION_ID.toString()))

  private fun repository(signRequest: Boolean?): RefreshableRelyingPartyRegistrationRepository {
    val properties = relyingPartyProperties(REGISTRATION_ID, identityProvider)
    properties.registration.getValue(REGISTRATION_ID).assertingparty.singlesignon.signRequest =
        signRequest
    return RefreshableRelyingPartyRegistrationRepository(properties).also { repository = it }
  }
}
