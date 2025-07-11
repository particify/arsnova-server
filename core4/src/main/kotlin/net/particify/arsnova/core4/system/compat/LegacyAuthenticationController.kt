/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.compat

import java.util.UUID
import net.particify.arsnova.core4.system.security.JwtUtils
import net.particify.arsnova.core4.user.User
import net.particify.arsnova.core4.user.UserService
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
class LegacyAuthenticationController(
    private val jwtUtils: JwtUtils,
    private val authenticationManager: AuthenticationManager,
    private val userService: UserService
) {
  @PostMapping("/login")
  fun currentAuthentication(@AuthenticationPrincipal user: User): LegacyAuthenticationResponse {
    val token = jwtUtils.encodeJwt(user.id!!.toString(), arrayOf("USER"))
    return LegacyAuthenticationResponse(
        user.id!!, user.username!!, user.mailAddress!!, "ARSNOVA", token)
  }

  @PostMapping("/login/registered")
  fun loginViaProvider(
      @RequestBody loginCredentials: LoginCredentials
  ): LegacyAuthenticationResponse {
    val loginId = loginCredentials.loginId.lowercase()
    val passwordToken = UsernamePasswordAuthenticationToken(loginId, loginCredentials.password)
    val authenticatedToken = authenticationManager.authenticate(passwordToken)
    val user = authenticatedToken.principal as User
    val token = jwtUtils.encodeJwt(user.id!!.toString(), arrayOf("USER"))
    return LegacyAuthenticationResponse(
        user.id!!, user.username!!, user.username!!, "ARSNOVA", token)
  }

  @PostMapping("/login/guest")
  fun createGuestAccount(): LegacyAuthenticationResponse {
    val user = userService.createAccount()
    val token = jwtUtils.encodeJwt(user.id!!.toString(), arrayOf("USER"))
    return LegacyAuthenticationResponse(user.id!!, authProvider = "ARSNOVA_GUEST", token = token)
  }

  data class LegacyAuthenticationResponse(
      val userId: UUID,
      val displayId: String? = null,
      val loginId: String? = null,
      val authProvider: String,
      val token: String
  )

  data class LoginCredentials(val loginId: String, val password: String)
}
