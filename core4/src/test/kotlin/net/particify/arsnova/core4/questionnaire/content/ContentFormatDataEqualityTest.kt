/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.content

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import tools.jackson.module.kotlin.jacksonObjectMapper

/**
 * Format data is compared to decide whether an update changes anything, so every format needs
 * structural equality. The copy is made the way the database makes one: through JSON.
 */
class ContentFormatDataEqualityTest {
  private val objectMapper = jacksonObjectMapper()

  private fun formatDataOfEveryFormat(): List<ContentFormatData> {
    val wordcloud = WordcloudContentFormatData(4)
    wordcloud.addBannedKeyword("spam")
    return listOf(ChoiceContentFormatData(listOf("A", "B"), listOf(1), true), wordcloud)
  }

  @Test
  fun shouldEqualItsCopy() {
    formatDataOfEveryFormat().forEach { original ->
      val copy =
          objectMapper.readValue(
              objectMapper.writeValueAsString(original), ContentFormatData::class.java)
      Assertions.assertNotSame(original, copy, original.format)
      Assertions.assertEquals(original, copy, original.format)
      Assertions.assertEquals(original.hashCode(), copy.hashCode(), original.format)
    }
  }
}
