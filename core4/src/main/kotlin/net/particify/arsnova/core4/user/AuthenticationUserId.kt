/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user

import java.util.UUID
import org.springframework.security.core.Authentication

/**
 * The ID of the account an authentication belongs to, or null if it belongs to none. Only an
 * [AuthenticatedUser] principal counts: a login carrying the account itself yields null, so it is
 * never mistaken for an authenticated request.
 */
fun Authentication?.userId(): UUID? = (this?.principal as? AuthenticatedUser)?.id
