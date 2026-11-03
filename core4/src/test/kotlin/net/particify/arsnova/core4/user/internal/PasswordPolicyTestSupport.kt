/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user.internal

import net.particify.arsnova.core4.common.ServiceProperties

private const val MIN_GUESSES_LOG10 = 10
private val DICTIONARY_LANGUAGES = listOf("en", "de")

/**
 * Everything a test does not pass carries the value shipped in `application.yaml`. Only the
 * estimator of those languages is shared with the test application contexts, so that one of other
 * languages is collected after its test.
 */
fun passwordPolicy(
    minGuessesLog10: Int = MIN_GUESSES_LOG10,
    contextWords: List<String> = listOf("arsnova", "particify"),
    rootUrl: String = "http://localhost",
    dictionaryLanguages: List<String> = DICTIONARY_LANGUAGES
) =
    PasswordPolicy(
        PasswordProperties(minGuessesLog10, contextWords, dictionaryLanguages),
        ServiceProperties("arsnova", rootUrl),
        if (dictionaryLanguages == DICTIONARY_LANGUAGES) ::passwordEstimator
        else ::unsharedPasswordEstimator)
