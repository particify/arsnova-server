/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.compat

import net.particify.arsnova.core4.system.config.LocalAccountPolicy
import net.particify.arsnova.core4.system.config.SecurityProperties
import net.particify.arsnova.core4.system.config.ServiceProperties
import net.particify.arsnova.core4.system.config.UiProperties
import net.particify.arsnova.core4.system.config.login
import net.particify.arsnova.core4.system.config.securityProperties
import net.particify.arsnova.core4.system.security.RoomCreationPolicy
import net.particify.arsnova.core4.user.internal.ExtendedSaml2RelyingPartyProperties
import net.particify.arsnova.core4.user.internal.LdapProperties
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

private const val REMEMBER_ME_ENABLED = "rememberMeEnabled"

/** Whether a client may offer to be remembered is decided by the configured lifetime alone. */
class LegacyConfigurationRememberMeTests {
  @Test
  fun shouldPublishSettingWithConfiguredLifetime() {
    Assertions.assertEquals(true, uiSettings(login())[REMEMBER_ME_ENABLED])
  }

  @Test
  fun shouldPublishNoSettingWithoutConfiguredLifetime() {
    val ui = uiSettings(login(rememberMeMaxAge = null))
    Assertions.assertFalse(ui.containsKey(REMEMBER_ME_ENABLED))
  }

  private fun uiSettings(login: SecurityProperties.Login): Map<String, Any> {
    val properties = securityProperties(login = login)
    val controller =
        LegacyConfigurationController(
            LdapProperties(),
            LocalAccountPolicy(properties),
            RoomCreationPolicy(properties),
            ExtendedSaml2RelyingPartyProperties(),
            properties,
            ServiceProperties("arsnova", "http://localhost"),
            UiProperties(mapOf()))
    return controller.configuration().ui
  }
}
