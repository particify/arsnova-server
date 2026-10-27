/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.common

import org.springframework.http.MediaType

enum class DelimitedTextFormat(val separator: Char, val mediaType: MediaType) {
  CSV(',', MediaType("text", "csv")),
  TSV('\t', MediaType("text", "tab-separated-values"));

  val extension: String = name.lowercase()

  companion object {
    fun fromParameter(value: String): DelimitedTextFormat? =
        entries.find { it.name == value.uppercase() }
  }
}
