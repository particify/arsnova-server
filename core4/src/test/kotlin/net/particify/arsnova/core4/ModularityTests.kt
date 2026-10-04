/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4

import com.tngtech.archunit.core.domain.JavaClass
import org.junit.jupiter.api.Test
import org.springframework.modulith.core.ApplicationModules
import org.springframework.modulith.docs.Documenter

class ModularityTests {
  private val modules =
      ApplicationModules.of(
          Core4Application::class.java,
          // A temporary importer that writes to other modules' persistence directly.
          JavaClass.Predicates.resideInAPackage("..system.migration.v3.."))

  @Test
  fun verifiesArchitecture() {
    modules.verify()
  }

  @Test
  fun createDocumentation() {
    Documenter(modules).writeDocumentation()
  }
}
