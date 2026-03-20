/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.migration.v3

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

data class RoomSettings(
    @field:JsonProperty("_id") override val id: String,
    override val creationTimestamp: Instant?,
    override val updateTimestamp: Instant?,
    val roomId: String,
    val commentThresholdEnabled: Boolean,
    val commentThreshold: Int,
    val commentTags: List<String>,
    val surveyEnabled: Boolean,
    val surveyType: SurveyType,
    val focusModeEnabled: Boolean
) : Entity {
  enum class SurveyType {
    FEEDBACK,
    SURVEY
  }
}
