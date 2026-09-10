/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user.exception

class MailAddressNotAllowedException(allowedDomains: List<String>) :
    RuntimeException(
        "Mail address domain not allowed. Allowed domains: ${allowedDomains.joinToString(", ")}")
