/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.migration.v3

import java.net.URI
import java.util.UUID
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "persistence.v3-migration")
data class MigrationProperties(
    val enabled: Boolean,
    val couchdbUrl: URI,
    val roomAccessUrl: URI,
    val authenticationProviderMapping: Map<String, UUID> = mapOf()
)
