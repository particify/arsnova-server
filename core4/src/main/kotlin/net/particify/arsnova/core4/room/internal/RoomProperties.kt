/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.room.internal

import net.particify.arsnova.core4.common.LanguageIso639
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "room")
data class RoomProperties(
    val demo: List<Demo> = emptyList(),
) {
  data class Demo(@field:LanguageIso639 val language: String, val shortId: Int)
}
