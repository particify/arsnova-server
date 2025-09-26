/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4

import org.junit.jupiter.api.Test
import org.springframework.modulith.core.ApplicationModules
import org.springframework.modulith.docs.Documenter

class ModularityTests {
  private val modules = ApplicationModules.of(Core4Application::class.java)

  @Test
  fun verifiesArchitecture() {
    modules.verify()
  }

  @Test
  fun createDocumentation() {
    Documenter(modules).writeDocumentation()
  }
}
