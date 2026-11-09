/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user.sso

import net.particify.arsnova.core4.user.User
import org.springframework.security.oauth2.core.oidc.user.OidcUser

/**
 * Carries the provisioned account through Spring Security's OpenID Connect login, whose principal
 * has to be an [OidcUser].
 */
class OidcLoginPrincipal(val user: User, oidcUser: OidcUser) : OidcUser by oidcUser
