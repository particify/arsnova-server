/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.api

import java.util.UUID
import net.particify.arsnova.core4.user.internal.ExtendedSaml2RelyingPartyProperties
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.servlet.view.RedirectView

@Controller
class SamlHttpController(private val saml2Properties: ExtendedSaml2RelyingPartyProperties) {
  @GetMapping("/auth/sso/{registrationId}")
  fun redirectToSamlLogin(@PathVariable registrationId: UUID): RedirectView {
    if (!saml2Properties.registration.containsKey(registrationId)) {
      error("Registration ID $registrationId does not exist.")
    }
    return RedirectView("/saml2/authenticate/$registrationId", true)
  }
}
