/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.content.api

import net.particify.arsnova.core4.questionnaire.content.ChoiceContentFormatData
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class ContentFormatDataInputTest {
  @Test
  fun shouldMapChoiceInputToChoiceFormatData() {
    val input =
        ContentFormatDataInput(
            choice =
                object : ContentFormatDataInput.ChoiceContentFormatDataInput {
                  override val choices = listOf("A", "B", "C")
                  override val correctOptionIndexes = listOf(0, 2)
                  override val multiple = true
                })

    Assertions.assertEquals(
        ChoiceContentFormatData(listOf("A", "B", "C"), listOf(0, 2), true), input.toFormatData())
  }

  @Test
  fun shouldRejectInputWithoutFormat() {
    Assertions.assertThrows(IllegalStateException::class.java) {
      ContentFormatDataInput(choice = null).toFormatData()
    }
  }
}
