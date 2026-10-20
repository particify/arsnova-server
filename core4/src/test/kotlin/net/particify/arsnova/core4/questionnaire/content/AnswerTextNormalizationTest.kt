/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.content

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class AnswerTextNormalizationTest {
  @Test
  fun shouldIgnoreCasePunctuationAndSpacing() {
    Assertions.assertEquals("newyork", normalizeAnswerText(" New-York! "))
    Assertions.assertEquals(normalizeAnswerText("New York"), normalizeAnswerText("newyork"))
  }

  @Test
  fun shouldKeepLettersOfEveryScriptAndDigits() {
    Assertions.assertEquals("straße42", normalizeAnswerText("Straße 42"))
    Assertions.assertEquals("ünïcödé", normalizeAnswerText("ÜNÏCÖDÉ"))
    Assertions.assertEquals("東京", normalizeAnswerText("東京!"))
  }

  @Test
  fun shouldNormalizeTextWithoutLettersOrDigitsToEmpty() {
    Assertions.assertEquals("", normalizeAnswerText(" ?!… "))
  }
}
