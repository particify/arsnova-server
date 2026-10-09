/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.security.api

import jakarta.servlet.http.HttpServletResponse
import java.util.UUID
import net.particify.arsnova.core4.user.sso.ExtendedSaml2RelyingPartyProperties
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.servlet.view.RedirectView

@Controller
class SamlHttpController(private val saml2Properties: ExtendedSaml2RelyingPartyProperties) {
  /**
   * An unknown registration is answered with `sendError` rather than an exception, so the error
   * page decides the representation: exception handlers would turn it into a problem document even
   * for a browser.
   */
  @GetMapping("/auth/sso/{registrationId}")
  fun redirectToSamlLogin(
      @PathVariable registrationId: UUID,
      response: HttpServletResponse
  ): RedirectView? {
    if (!saml2Properties.registration.containsKey(registrationId)) {
      response.sendError(HttpStatus.NOT_FOUND.value())
      return null
    }
    return RedirectView("/saml2/authenticate/$registrationId", true)
  }
}
