/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.compat

import java.util.UUID
import net.particify.arsnova.core4.system.security.JwtUtils
import net.particify.arsnova.core4.user.User
import net.particify.arsnova.core4.user.UserService
import net.particify.arsnova.core4.user.internal.ExtendedSaml2RelyingPartyProperties
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.view.RedirectView

@RestController
@RequestMapping("/auth")
class LegacyAuthenticationController(
    private val jwtUtils: JwtUtils,
    private val authenticationManager: AuthenticationManager,
    private val userService: UserService,
    private val saml2Properties: ExtendedSaml2RelyingPartyProperties
) {
  @PostMapping("/login")
  fun currentAuthentication(@AuthenticationPrincipal user: User): LegacyAuthenticationResponse {
    val token = jwtUtils.encodeJwt(user.id!!.toString(), listOf("USER"))
    return LegacyAuthenticationResponse(
        user.id!!, "ARSNOVA", token, user.username, user.mailAddress, user.displayName())
  }

  @PostMapping("/login/registered")
  fun loginViaProvider(
      @RequestBody loginCredentials: LoginCredentials
  ): LegacyAuthenticationResponse {
    val loginId = loginCredentials.loginId.lowercase()
    val passwordToken = UsernamePasswordAuthenticationToken(loginId, loginCredentials.password)
    val authenticatedToken = authenticationManager.authenticate(passwordToken)
    val user = authenticatedToken.principal as User
    val token = jwtUtils.encodeJwt(user.id!!.toString(), listOf("USER"))
    return LegacyAuthenticationResponse(
        user.id!!, "ARSNOVA", token, user.username!!, user.username!!, user.displayName())
  }

  @PostMapping("/login/guest")
  fun createGuestAccount(): LegacyAuthenticationResponse {
    val user = userService.createAccount()
    val token = jwtUtils.encodeJwt(user.id!!.toString(), listOf("USER"))
    return LegacyAuthenticationResponse(user.id!!, "ARSNOVA_GUEST", token)
  }

  @GetMapping("/sso/{registrationId}")
  fun redirectToSamlLogin(@PathVariable registrationId: UUID): RedirectView {
    if (!saml2Properties.registration.containsKey(registrationId)) {
      error("Registration ID $registrationId does not exist.")
    }
    return RedirectView("/saml2/authenticate/$registrationId", true)
  }

  data class LegacyAuthenticationResponse(
      val userId: UUID,
      val authProvider: String,
      val token: String,
      val loginId: String? = null,
      val displayId: String? = null,
      val displayName: String? = null,
  )

  data class LoginCredentials(val loginId: String, val password: String)
}
