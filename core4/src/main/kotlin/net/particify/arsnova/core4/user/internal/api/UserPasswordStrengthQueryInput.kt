/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user.internal.api

data class UserPasswordStrengthQueryInput(val password: String, val mailAddress: String?)
