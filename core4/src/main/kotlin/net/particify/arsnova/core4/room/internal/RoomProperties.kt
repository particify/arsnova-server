/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.room.internal

import java.time.Duration
import java.time.temporal.ChronoUnit
import net.particify.arsnova.core4.common.LanguageIso639
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.convert.DurationUnit

private const val DEFAULT_ACTIVITY_MIN_MEMBER_COUNT = 10
private const val DEFAULT_ACTIVITY_WINDOW_MINUTES = 5L

@ConfigurationProperties(prefix = "room")
data class RoomProperties(
    val activity: Activity = Activity(),
    val demo: List<Demo> = emptyList(),
) {
  data class Activity(
      val minMemberCount: Int = DEFAULT_ACTIVITY_MIN_MEMBER_COUNT,
      @param:DurationUnit(ChronoUnit.MINUTES)
      val window: Duration = Duration.ofMinutes(DEFAULT_ACTIVITY_WINDOW_MINUTES),
  )

  data class Demo(@field:LanguageIso639 val language: String, val shortId: Int)
}
