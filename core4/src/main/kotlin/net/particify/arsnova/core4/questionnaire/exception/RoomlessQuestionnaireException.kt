/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.exception

import java.util.UUID

/** Raised for operations which only make sense in a room, attempted on a template's contents. */
class RoomlessQuestionnaireException(val id: UUID?) :
    RuntimeException("Operation not supported for a questionnaire without a room")
