/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.exception

import java.util.UUID

class AbstentionsNotAllowedException(val contentId: UUID?) :
    RuntimeException("The content does not allow abstentions")
