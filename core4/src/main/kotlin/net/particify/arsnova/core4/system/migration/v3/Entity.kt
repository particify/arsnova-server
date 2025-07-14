/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.migration.v3

import java.time.Instant

interface Entity {
  val id: String
  val creationTimestamp: Instant
  val updateTimestamp: Instant?
}
