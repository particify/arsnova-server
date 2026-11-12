/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.security.internal

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository

/**
 * Keeps no authorized client. A login only needs the provider's tokens until the account is
 * provisioned, and the default would hold every user's tokens in memory for good.
 */
object DiscardingOAuth2AuthorizedClientRepository : OAuth2AuthorizedClientRepository {
  override fun <T : OAuth2AuthorizedClient> loadAuthorizedClient(
      clientRegistrationId: String,
      principal: Authentication,
      request: HttpServletRequest
  ): T? = null

  override fun saveAuthorizedClient(
      authorizedClient: OAuth2AuthorizedClient,
      principal: Authentication,
      request: HttpServletRequest,
      response: HttpServletResponse
  ) = Unit

  override fun removeAuthorizedClient(
      clientRegistrationId: String,
      principal: Authentication,
      request: HttpServletRequest,
      response: HttpServletResponse
  ) = Unit
}
