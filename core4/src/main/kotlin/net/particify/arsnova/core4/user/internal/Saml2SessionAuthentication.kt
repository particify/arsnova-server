/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user.internal

import java.time.Instant
import org.springframework.security.core.AuthenticatedPrincipal
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication

/**
 * Carries the instant at which the identity provider itself ends the session, which a stock
 * [Saml2Authentication] has no room for. The value is in scope only while the SAML response is
 * being converted, and the `details` of an authentication cannot hold it either: the authentication
 * provider overwrites those with the filter's own as soon as the converter returns.
 *
 * [endsAt] is absent for an identity provider which asserts no bound on the session, which most do
 * not.
 */
class Saml2SessionAuthentication(
    principal: AuthenticatedPrincipal,
    saml2Response: String,
    authorities: Collection<GrantedAuthority>,
    val endsAt: Instant?
) : Saml2Authentication(principal, saml2Response, authorities)
