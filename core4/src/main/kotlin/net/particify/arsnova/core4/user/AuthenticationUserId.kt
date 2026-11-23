/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user

import java.util.UUID
import org.springframework.security.core.Authentication

/** The ID of the account an authentication belongs to, or null if it belongs to none. */
fun Authentication?.userId(): UUID? =
    when (val principal = this?.principal) {
      is AuthenticatedUser -> principal.id
      is User -> principal.id
      else -> null
    }
