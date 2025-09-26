/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.migration.v3

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

data class Announcement(
    @field:JsonProperty("_id") override val id: String,
    override val creationTimestamp: Instant,
    override val updateTimestamp: Instant?,
    val roomId: String,
    val title: String,
    val body: String,
    val creatorId: String,
) : Entity
