/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.event

import java.util.UUID
import org.jmolecules.event.annotation.DomainEvent

@DomainEvent
class QuestionnaireDuplicatedEvent(
    val originalQuestionnaireId: UUID,
    val duplicatedQuestionnaireId: UUID
) : QuestionnaireEvent
