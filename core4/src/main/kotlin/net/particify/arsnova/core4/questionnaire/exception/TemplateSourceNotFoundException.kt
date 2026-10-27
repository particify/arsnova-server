/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.exception

import java.util.UUID

/** The questionnaire a template was created from no longer exists. */
class TemplateSourceNotFoundException(val id: UUID?) :
    RuntimeException("Source questionnaire of template not found")
