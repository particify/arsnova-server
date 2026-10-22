/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user.internal

import jakarta.validation.constraints.PositiveOrZero
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

/**
 * Shares its prefix with the bcrypt setting in `SecurityProperties`, which the user module must not
 * depend on.
 */
@ConfigurationProperties(prefix = "security.password")
@Validated
data class PasswordProperties(
    /**
     * The decimal logarithm of the number of guesses a password has to withstand, as estimated by
     * zxcvbn. Each step multiplies the required number of guesses by ten.
     */
    @field:PositiveOrZero val minGuessesLog10: Int,
    /**
     * Words specific to the service which do not make a password any harder to guess. The host name
     * of `service.root-url` is added to them.
     */
    val contextWords: List<String> = listOf(),
    /**
     * Languages, as ISO 639-1 codes, whose word lists the estimate checks a password against
     * besides the ones belonging to no language.
     */
    val dictionaryLanguages: List<String> = listOf()
)
