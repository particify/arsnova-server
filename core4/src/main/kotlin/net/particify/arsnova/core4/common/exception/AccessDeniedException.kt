/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.common.exception

/**
 * This exception is used to deny access from the domain layer. Prefer using Spring Security's
 * annotations instead when possible.
 */
data class AccessDeniedException(override val message: String) : RuntimeException(message)
