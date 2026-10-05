/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user.internal

fun localAccount(
    enabled: Boolean = true,
    selfRegistrationEnabled: Boolean = true,
    allowedMailAddressDomains: List<String> = listOf()
) = LocalAccountProperties(enabled, selfRegistrationEnabled, allowedMailAddressDomains)
