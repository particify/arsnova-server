/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.violationreport

import org.springframework.modulith.ApplicationModule
import org.springframework.modulith.PackageInfo

/**
 * Takes reports on entities of other modules without depending on them: those modules register
 * their entities as targets and handle the decisions on reports targeting them.
 */
@PackageInfo @ApplicationModule(allowedDependencies = ["common"]) class ModuleMetadata
