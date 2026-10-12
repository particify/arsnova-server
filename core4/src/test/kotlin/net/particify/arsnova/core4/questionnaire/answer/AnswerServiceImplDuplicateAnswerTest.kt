/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.answer

import java.util.Optional
import java.util.UUID
import net.particify.arsnova.core4.questionnaire.content.ChoiceContentFormatData
import net.particify.arsnova.core4.questionnaire.content.Content
import net.particify.arsnova.core4.questionnaire.content.ContentServiceImpl
import net.particify.arsnova.core4.questionnaire.exception.AnswerAlreadyExistsException
import net.particify.arsnova.core4.questionnaire.questionnaire.Questionnaire
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnaireRepository
import net.particify.arsnova.core4.questionnaire.round.Round
import net.particify.arsnova.core4.questionnaire.round.RoundRepository
import net.particify.arsnova.core4.questionnaire.round.api.RoundStatsEventPublisher
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.AuditorAware

private val ROUND_ID = UUID.randomUUID()
private val USER_ID = UUID.randomUUID()

/** The check before storing cannot see a concurrent submission; the unique index catches it. */
class AnswerServiceImplDuplicateAnswerTest {
  private val answerRepository = Mockito.mock(AnswerRepository::class.java)
  private val roundRepository = Mockito.mock(RoundRepository::class.java)
  private val answerService =
      AnswerServiceImpl(
          answerRepository,
          roundRepository,
          Mockito.mock(ContentServiceImpl::class.java),
          RoundStatsEventPublisher(),
          Mockito.mock(QuestionnaireRepository::class.java),
          AuditorAware { Optional.of(USER_ID) })

  init {
    val content =
        Content(
            id = UUID.randomUUID(),
            questionnaire = Questionnaire(id = UUID.randomUUID()),
            formatData = ChoiceContentFormatData(listOf("A", "B"), null, false))
    Mockito.`when`(roundRepository.findById(ROUND_ID))
        .thenReturn(Optional.of(Round(id = ROUND_ID, content = content)))
  }

  @Test
  fun shouldReportViolationOfTheUniqueIndexAsDuplicateAnswer() {
    Mockito.`when`(answerRepository.saveAndFlush(Mockito.any(Answer::class.java)))
        .thenThrow(DataIntegrityViolationException("duplicate key"))
    val thrown =
        Assertions.assertThrows(AnswerAlreadyExistsException::class.java) {
          answerService.createAnswer(ROUND_ID, ChoiceAnswerFormatData(listOf(0)))
        }
    Assertions.assertEquals(ROUND_ID, thrown.roundId)
    Assertions.assertInstanceOf(DataIntegrityViolationException::class.java, thrown.cause)
  }

  @Test
  fun shouldRejectAbstentionOfUserWhoAnswered() {
    Mockito.`when`(answerRepository.findByRoundIdAndAuditMetadataCreatedBy(ROUND_ID, USER_ID))
        .thenReturn(Answer(id = UUID.randomUUID()))
    Assertions.assertThrows(AnswerAlreadyExistsException::class.java) {
      answerService.createAbstention(ROUND_ID)
    }
    Mockito.verify(answerRepository, Mockito.never()).saveAndFlush(Mockito.any())
  }
}
