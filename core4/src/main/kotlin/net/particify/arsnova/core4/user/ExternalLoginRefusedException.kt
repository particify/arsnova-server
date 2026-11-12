/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user

import org.springframework.security.core.AuthenticationException

/**
 * Refuses an external login from an [ExternalLoginHook]. Of the two, only [errorCode] is shown to
 * the user.
 */
class ExternalLoginRefusedException(val errorCode: String, message: String) :
    AuthenticationException(message)
