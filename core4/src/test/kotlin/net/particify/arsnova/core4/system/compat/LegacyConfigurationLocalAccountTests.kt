/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.compat

import net.particify.arsnova.core4.system.config.LocalAccountPolicy
import net.particify.arsnova.core4.system.config.SecurityProperties
import net.particify.arsnova.core4.system.config.ServiceProperties
import net.particify.arsnova.core4.system.config.UiProperties
import net.particify.arsnova.core4.system.config.localAccount
import net.particify.arsnova.core4.system.config.securityProperties
import net.particify.arsnova.core4.system.security.RoomCreationPolicy
import net.particify.arsnova.core4.user.internal.ExtendedSaml2RelyingPartyProperties
import net.particify.arsnova.core4.user.internal.LdapProperties
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

private const val LOCAL_PROVIDER_ID = "user-db"
private const val REGISTRATION_DISABLED = "registrationDisabled"

/** What the local account settings change in the payload the v3 clients read. */
class LegacyConfigurationLocalAccountTests {
  @Test
  fun shouldPublishLocalProviderWhileEnabled() {
    Assertions.assertTrue(providerIds(localAccount()).contains(LOCAL_PROVIDER_ID))
  }

  @Test
  fun shouldOmitLocalProviderWhileDisabled() {
    Assertions.assertFalse(providerIds(localAccount(enabled = false)).contains(LOCAL_PROVIDER_ID))
  }

  @Test
  fun shouldPublishNoRegistrationSettingWhileSelfRegistrationIsEnabled() {
    Assertions.assertFalse(configuration(localAccount()).ui.containsKey(REGISTRATION_DISABLED))
  }

  @Test
  fun shouldDisableRegistrationWithoutSelfRegistration() {
    val ui = configuration(localAccount(selfRegistrationEnabled = false)).ui
    Assertions.assertEquals(true, ui[REGISTRATION_DISABLED])
  }

  @Test
  fun shouldDisableRegistrationWhileLocalAccountsAreDisabled() {
    val ui = configuration(localAccount(enabled = false)).ui
    Assertions.assertEquals(true, ui[REGISTRATION_DISABLED])
  }

  /** An operator who disabled registration by hand keeps that, self-registration or not. */
  @Test
  fun shouldKeepManuallyDisabledRegistration() {
    val ui = configuration(localAccount(), mapOf(REGISTRATION_DISABLED to true)).ui
    Assertions.assertEquals(true, ui[REGISTRATION_DISABLED])
  }

  private fun providerIds(localAccount: SecurityProperties.LocalAccount) =
      configuration(localAccount).authenticationProviders.map { it.id }

  private fun configuration(
      localAccount: SecurityProperties.LocalAccount,
      ui: Map<String, Any> = mapOf()
  ) =
      LegacyConfigurationController(
              LdapProperties(),
              LocalAccountPolicy(securityProperties(localAccount = localAccount)),
              RoomCreationPolicy(securityProperties()),
              ExtendedSaml2RelyingPartyProperties(),
              ServiceProperties("arsnova", "http://localhost"),
              UiProperties(ui))
          .configuration()
}
