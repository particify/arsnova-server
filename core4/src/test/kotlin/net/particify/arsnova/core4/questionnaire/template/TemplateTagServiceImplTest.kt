/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.template

import java.util.Optional
import java.util.UUID
import net.particify.arsnova.core4.common.exception.InvalidInputException
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.dao.DataIntegrityViolationException

// Mockito's matchers return null, which the non-null parameters of a Kotlin interface reject.
private fun anyUuid(): UUID = Mockito.any(UUID::class.java) ?: UUID(0, 0)

private fun eq(value: String): String = Mockito.eq(value) ?: value

class TemplateTagServiceImplTest {
  private val repository = Mockito.mock(TemplateTagRepository::class.java)
  private val service = TemplateTagServiceImpl(repository)

  @Test
  fun shouldReturnTheTagWhichAConcurrentRequestCreatedFirst() {
    val winner = TemplateTag(id = UUID.randomUUID(), language = "en", name = "geography")
    // Unknown when looked up, but already there once the insert is attempted.
    Mockito.`when`(repository.findByLanguageAndNameIgnoreCase("en", "Geography"))
        .thenReturn(null, winner)
    val tags = service.findOrCreateByNames("en", listOf("Geography"))
    Assertions.assertEquals(listOf(winner.id), tags.map { it.id })
    Mockito.verify(repository).insertIfAbsent(anyUuid(), eq("en"), eq("Geography"))
    Mockito.verify(repository, Mockito.never()).save(Mockito.any())
  }

  @Test
  fun shouldRejectARenameWhichLosesTheRaceForTheName() {
    val id = UUID.randomUUID()
    val tag = TemplateTag(id = id, language = "en", name = "Maps")
    Mockito.`when`(repository.findById(id)).thenReturn(Optional.of(tag))
    Mockito.`when`(repository.saveAndFlush(tag))
        .thenThrow(DataIntegrityViolationException("tag_language_name_idx"))
    Assertions.assertThrows(InvalidInputException::class.java) {
      service.updateName(id, "Geography")
    }
  }
}
