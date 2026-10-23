/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user.internal

import net.particify.arsnova.core4.common.ServiceProperties

private const val MIN_GUESSES_LOG10 = 10

/** Everything a test does not pass carries the value shipped in `application.yaml`. */
fun passwordPolicy(
    minGuessesLog10: Int = MIN_GUESSES_LOG10,
    contextWords: List<String> = listOf("arsnova", "particify"),
    rootUrl: String = "http://localhost"
) =
    PasswordPolicy(
        PasswordProperties(minGuessesLog10, contextWords), ServiceProperties("arsnova", rootUrl))
