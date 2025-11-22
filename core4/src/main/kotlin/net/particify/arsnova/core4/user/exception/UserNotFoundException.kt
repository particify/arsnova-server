/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user.exception

import java.util.UUID

class UserNotFoundException(val id: UUID?) : RuntimeException("User not found") {
  constructor() : this(null)
}
