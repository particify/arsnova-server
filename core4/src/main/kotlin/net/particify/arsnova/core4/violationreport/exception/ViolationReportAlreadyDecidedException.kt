/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.violationreport.exception

import java.util.UUID

class ViolationReportAlreadyDecidedException(val id: UUID) :
    RuntimeException("Violation report has already been decided")
