/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.exception

class RoundsNotSupportedException(val format: String) :
    RuntimeException("Content format $format does not support new rounds")
