/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.violationreport.exception

import java.util.UUID

class ViolationReportNotFoundException(val id: UUID) :
    RuntimeException("Violation report not found")
