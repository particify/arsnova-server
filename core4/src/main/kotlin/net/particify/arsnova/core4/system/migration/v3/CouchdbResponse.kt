/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.migration.v3

data class CouchdbResponse<T : Entity>(val rows: List<Row<T>>) {
  data class Row<T>(val doc: T)
}
