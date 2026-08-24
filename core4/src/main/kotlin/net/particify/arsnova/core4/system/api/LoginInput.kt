/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.api

import java.util.UUID

/** [providerId] is absent for the local provider. */
data class LoginInput(val username: String, val password: String, val providerId: UUID? = null)
