/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.exception

import java.util.UUID

class AnswerAlreadyExistsException(val roundId: UUID?, cause: Throwable? = null) :
    RuntimeException("The user has already answered this round", cause)
