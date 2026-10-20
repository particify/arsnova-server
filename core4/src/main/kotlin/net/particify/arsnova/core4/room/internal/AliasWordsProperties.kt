/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.room.internal

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "alias-words")
data class AliasWordsProperties(
    val adjectives: List<String> = emptyList(),
    val nouns: List<String> = emptyList(),
    val specialNouns: List<String> = emptyList(),
)
