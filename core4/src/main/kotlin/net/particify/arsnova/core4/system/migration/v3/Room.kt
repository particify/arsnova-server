/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.migration.v3

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

data class Room(
    @field:JsonProperty("_id") override val id: String,
    override val creationTimestamp: Instant,
    override val updateTimestamp: Instant?,
    val shortId: String,
    val name: String,
    val description: String = "",
    val ownerId: String,
    val importMetadata: ImportMetadata?
) : Entity {
  data class ImportMetadata(val source: String, val timestamp: Instant)
}
