/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire

import org.springframework.modulith.ApplicationModule
import org.springframework.modulith.PackageInfo

@PackageInfo
@ApplicationModule(
    allowedDependencies =
        [
            "common",
            "license",
            "room",
            "room::events",
            "security::permission",
            "user",
            "user::events"])
class ModuleMetadata
