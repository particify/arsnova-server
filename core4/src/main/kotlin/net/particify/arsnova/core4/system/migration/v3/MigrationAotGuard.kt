/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.migration.v3

import org.springframework.aot.AotDetector
import org.springframework.stereotype.Component

/**
 * The migration's beans are conditional on its property, and AOT processing decides those
 * conditions at build time, where the migration is disabled. Without this, enabling it on an AOT
 * build would silently start the application normally instead.
 */
@Component
class MigrationAotGuard(properties: MigrationProperties) {
  init {
    check(!properties.enabled || !AotDetector.useGeneratedArtifacts()) {
      "The v3 migration cannot run with AOT-generated artifacts. " +
          "Run it on the JVM without spring.aot.enabled."
    }
  }
}
