/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.migration.v3

import java.time.Instant
import java.util.UUID
import java.util.random.RandomGenerator
import java.util.regex.Pattern

object UuidHelper {
  private const val REPLACEMENT_PATTERN = "$1-$2-$3-$4-$5"
  private const val UUIDV7_MAX_RANDOM_SEQUENCE = 0x3FFFFFFFFFFFFFFFL
  private const val BITSHIFT_MILLIS = 16
  private const val BITMASK_48_MSB = -0x10000L
  private const val BITMASK_12_LSB = 0xFFFL
  private const val BITMASK_VERSION_7 = 0x7000L
  private val BITMASK_VARIANT_4 = 0x8000000000000000u.toLong()
  private val pattern: Pattern = Pattern.compile("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})")

  fun stringToUuid(uuidString: String?): UUID? {
    if (uuidString.isNullOrEmpty()) {
      return null
    }
    return UUID.fromString(pattern.matcher(uuidString).replaceFirst(REPLACEMENT_PATTERN))
  }

  /**
   * Generates version 7 UUID for preexisting, timestamped data. It operates stateless and therefore
   * cannot guarantee monotonicity for consecutive calls with the same timestamp. It should not be
   * used to generate UUIDs with a current timestamp.
   *
   * Adopted from Hibernate's UuidVersion7Strategy.
   *
   * SPDX-License-Identifier: Apache-2.0 Copyright Red Hat Inc. and Hibernate Authors
   *
   * @param timestamp Used for the timestamp part of the UUID
   * @param randomGenerator A cryptographically secure random number generator
   */
  fun generateUuidV7(timestamp: Instant, randomGenerator: RandomGenerator): UUID {
    return UUID(
        // MSB bits 0-47 - 48-bit big-endian unsigned number of the Unix Epoch timestamp in
        // milliseconds
        timestamp.toEpochMilli() shl
            BITSHIFT_MILLIS and
            BITMASK_48_MSB
            // MSB bits 48-51 - version = 7
            or
            BITMASK_VERSION_7
            // MSB bits 52-63 - sub-milliseconds part of timestamp
            or
            (nanos(timestamp) and BITMASK_12_LSB),
        // LSB bits 0-1 - variant = 4
        BITMASK_VARIANT_4
        // LSB bits 2-63 - pseudorandom counter
        or randomGenerator.nextLong(UUIDV7_MAX_RANDOM_SEQUENCE + 1))
  }

  /**
   * Sub-milliseconds part of timestamp (micro- and nanoseconds) mapped to 12 bit integral value.
   * Calculated as nanos / 1000000 * 4096.
   */
  @Suppress("MagicNumber")
  private fun nanos(timestamp: Instant): Long {
    return ((timestamp.nano % 1000000L) * 0.004096).toLong()
  }
}
