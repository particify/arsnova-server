/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.migration.v3

import java.util.UUID
import java.util.regex.Pattern

object UuidHelper {
  private const val REPLACEMENT_PATTERN = "$1-$2-$3-$4-$5"
  private val pattern: Pattern = Pattern.compile("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})")

  fun stringToUuid(uuidString: String?): UUID? {
    if (uuidString.isNullOrEmpty()) {
      return null
    }
    return UUID.fromString(pattern.matcher(uuidString).replaceFirst(REPLACEMENT_PATTERN))
  }
}
