/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.common

import java.net.URI
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "common.text-rendering")
data class TextRenderingProperties(val enabled: Boolean, val serviceUrl: URI)
