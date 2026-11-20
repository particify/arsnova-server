/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.migration.v3

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

data class ViolationReport(
    @field:JsonProperty("_id") override val id: String,
    override val creationTimestamp: Instant,
    override val updateTimestamp: Instant?,
    val targetType: String,
    val targetId: String,
    val reason: String,
    val description: String,
    val creatorId: String,
    val decision: String?,
) : Entity
