/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.common.exception

class InvalidInputException(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause)
