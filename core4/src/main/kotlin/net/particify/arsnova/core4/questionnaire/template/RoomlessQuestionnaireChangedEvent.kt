/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.template

import java.util.UUID
import net.particify.arsnova.core4.questionnaire.event.QuestionnaireEvent
import org.jmolecules.event.annotation.DomainEvent

/**
 * The contents or settings of a room-less questionnaire, which holds a template's contents and
 * shares its id, were changed.
 */
@DomainEvent data class RoomlessQuestionnaireChangedEvent(val id: UUID) : QuestionnaireEvent
