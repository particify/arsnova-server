/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user.exception

import java.util.UUID

class InvalidUserStateException(message: String?, val id: UUID) : RuntimeException(message)
