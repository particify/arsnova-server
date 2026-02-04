/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.migration.v3

import org.springframework.core.ParameterizedTypeReference

object MigrationHelper {
  inline fun <reified T : Any> typeReference() = object : ParameterizedTypeReference<T>() {}
}
