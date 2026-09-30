/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.exception

import java.util.UUID

class TemplateNotFoundException(val id: UUID?) : RuntimeException("Template not found")
