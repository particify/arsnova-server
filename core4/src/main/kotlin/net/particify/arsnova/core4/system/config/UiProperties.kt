/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties data class UiProperties(val ui: Map<String, Any> = mapOf())
