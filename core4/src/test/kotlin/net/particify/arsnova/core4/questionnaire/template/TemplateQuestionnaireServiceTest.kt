/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.template

import java.util.Optional
import java.util.UUID
import net.particify.arsnova.core4.license.License
import net.particify.arsnova.core4.questionnaire.content.ChoiceContentFormatData
import net.particify.arsnova.core4.questionnaire.content.Content
import net.particify.arsnova.core4.questionnaire.content.ContentRepository
import net.particify.arsnova.core4.questionnaire.content.ContentServiceImpl
import net.particify.arsnova.core4.questionnaire.content.TemplateCredit
import net.particify.arsnova.core4.questionnaire.content.TextContentFormatData
import net.particify.arsnova.core4.questionnaire.content.WordcloudContentFormatData
import net.particify.arsnova.core4.questionnaire.exception.QuestionnaireNotFoundException
import net.particify.arsnova.core4.questionnaire.exception.QuestionnaireWithNameAlreadyExistsException
import net.particify.arsnova.core4.questionnaire.exception.RoomlessQuestionnaireException
import net.particify.arsnova.core4.questionnaire.questionnaire.Questionnaire
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnairePublishingMode
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnaireServiceImpl
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnaireType
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.data.domain.ScrollPosition
import org.springframework.data.domain.Window

private val ROOM_ID = UUID.randomUUID()

/** Mockito.any() for a non-null Kotlin parameter, which a plain null would fail at the call. */
private fun <T> anyOf(type: Class<T>): T {
  Mockito.any(type)
  @Suppress("UNCHECKED_CAST")
  return null as T
}

private const val NAME = "Template"

class TemplateQuestionnaireServiceTest {
  private val questionnaireService = Mockito.mock(QuestionnaireServiceImpl::class.java)
  private val contentService = Mockito.mock(ContentServiceImpl::class.java)
  private val contentRepository = Mockito.mock(ContentRepository::class.java)
  private val templatingService =
      TemplateQuestionnaireService(questionnaireService, contentService, contentRepository)

  init {
    Mockito.`when`(questionnaireService.save(Mockito.any(Questionnaire::class.java))).thenAnswer {
      it.arguments[0]
    }
    Mockito.`when`(questionnaireService.saveAndFlush(Mockito.any(Questionnaire::class.java)))
        .thenAnswer { it.arguments[0] }
  }

  private fun stored(roomId: UUID?, vararg contents: Content): Questionnaire {
    val questionnaire =
        Questionnaire(id = UUID.randomUUID(), roomId = roomId, contents = contents.toMutableList())
    contents.forEach { it.questionnaire = questionnaire }
    Mockito.`when`(questionnaireService.findById(questionnaire.id!!))
        .thenReturn(Optional.of(questionnaire))
    return questionnaire
  }

  private fun bannedWordcloud(): WordcloudContentFormatData {
    val formatData = WordcloudContentFormatData(5)
    formatData.addBannedKeyword("spam")
    return formatData
  }

  private val earlierTemplateId = UUID.randomUUID()

  /** The contents also carry the provenance of a copy made from an earlier template. */
  private fun runtimeStateContents() =
      arrayOf(
          Content(
              id = UUID.randomUUID(),
              formatData = bannedWordcloud(),
              sourceTemplateId = earlierTemplateId,
              sourceContentId = UUID.randomUUID(),
              sourceTemplateLicenseId = "CC0-1.0",
              sourceTemplateAttribution = "Earlier"),
          Content(
              id = UUID.randomUUID(),
              formatData = TextContentFormatData(true),
              sourceTemplateId = earlierTemplateId,
              sourceContentId = UUID.randomUUID(),
              sourceTemplateLicenseId = "CC0-1.0",
              sourceTemplateAttribution = "Earlier"))

  private fun withEarlierProvenance(questionnaire: Questionnaire): Questionnaire {
    questionnaire.sourceTemplateId = earlierTemplateId
    return questionnaire
  }

  private fun assertNoProvenance(questionnaire: Questionnaire) {
    Assertions.assertNull(questionnaire.sourceTemplateId)
    questionnaire.contents.forEach {
      Assertions.assertNull(it.sourceTemplateId)
      Assertions.assertNull(it.sourceContentId)
      Assertions.assertNull(it.sourceTemplateLicenseId)
      Assertions.assertNull(it.sourceTemplateAttribution)
    }
  }

  private fun assertRuntimeStateReset(contents: List<Content>) {
    Assertions.assertEquals(
        listOf<String>(), (contents[0].formatData as WordcloudContentFormatData).bannedKeywords)
    Assertions.assertEquals(5, (contents[0].formatData as WordcloudContentFormatData).maxAnswers)
    Assertions.assertEquals(TextContentFormatData(false), contents[1].formatData)
  }

  /** Lets the mocked duplication behave like the real one as far as the contents list goes. */
  private fun duplicateInto(copy: Questionnaire) {
    Mockito.`when`(
            questionnaireService.duplicate(
                anyOf(Questionnaire::class.java), Mockito.any(), Mockito.anyString()))
        .thenAnswer {
          val original = it.arguments[0] as Questionnaire
          copy.contents =
              original.contents
                  .map { content ->
                    content.copy().also { duplicated -> duplicated.questionnaire = copy }
                  }
                  .toMutableList()
          copy.roomId = it.arguments[1] as UUID?
          copy.name = it.arguments[2] as String
          copy
        }
  }

  @Test
  fun shouldCopyARoomQuestionnaireIntoATemplateWithoutRuntimeState() {
    val original = stored(ROOM_ID, *runtimeStateContents())
    val copy = withEarlierProvenance(Questionnaire(id = UUID.randomUUID()))
    duplicateInto(copy)

    Assertions.assertEquals(copy.id, templatingService.createRoomlessCopy(original.id!!, NAME))

    Mockito.verify(questionnaireService).duplicate(original, null, NAME)
    assertRuntimeStateReset(copy.contents)
    assertNoProvenance(copy)
    Mockito.verify(contentRepository, Mockito.times(2)).save(Mockito.any())
    Assertions.assertEquals(
        listOf("spam"),
        (original.contents[0].formatData as WordcloudContentFormatData).bannedKeywords)
  }

  @Test
  fun shouldMoveTheCreditOfACopiedContentUpstreamInATemplate() {
    val upstream = TemplateCredit(UUID.randomUUID(), "CC0-1.0", "First")
    val contents = runtimeStateContents()
    contents[0].upstreamAttributions = listOf(upstream)
    contents[1].upstreamAttributions =
        listOf(TemplateCredit(earlierTemplateId, "CC0-1.0", "Earlier"), upstream)
    val original = stored(ROOM_ID, *contents)
    val copy = Questionnaire(id = UUID.randomUUID())
    duplicateInto(copy)

    templatingService.createRoomlessCopy(original.id!!, NAME)

    val earlier = TemplateCredit(earlierTemplateId, "CC0-1.0", "Earlier")
    Assertions.assertEquals(
        listOf(listOf(earlier, upstream), listOf(earlier, upstream)),
        copy.contents.map { it.upstreamAttributions })
    assertNoProvenance(copy)
  }

  @Test
  fun shouldOnlyCopyRoomQuestionnairesIntoTemplates() {
    val roomless = stored(null)
    Assertions.assertThrows(RoomlessQuestionnaireException::class.java) {
      templatingService.createRoomlessCopy(roomless.id!!, NAME)
    }
    Assertions.assertThrows(QuestionnaireNotFoundException::class.java) {
      templatingService.createRoomlessCopy(UUID.randomUUID(), NAME)
    }
  }

  @Test
  fun shouldReplaceTheTemplatesContentsAndSettingsWithTheSources() {
    val old =
        Content(
            id = UUID.randomUUID(), formatData = ChoiceContentFormatData(listOf("A"), null, false))
    val roomless = withEarlierProvenance(stored(null, old))
    roomless.activeRound = net.particify.arsnova.core4.questionnaire.round.Round()
    val source = stored(ROOM_ID, *runtimeStateContents())
    source.publishingMode = QuestionnairePublishingMode.UP_TO
    source.statisticsPublished = true
    source.correctOptionsPublished = false
    source.leaderboardEnabled = true
    Mockito.`when`(contentService.delete(old.id!!)).thenAnswer {
      roomless.contents.remove(old)
      old.id
    }
    Mockito.`when`(contentService.duplicate(anyOf(Content::class.java), Mockito.any())).thenAnswer {
      val duplicated = (it.arguments[0] as Content).copy()
      roomless.contents.add(duplicated)
      duplicated
    }

    templatingService.replaceRoomlessContents(roomless.id!!, source.id!!)

    Assertions.assertEquals(2, roomless.contents.size)
    assertRuntimeStateReset(roomless.contents)
    assertNoProvenance(roomless)
    Assertions.assertEquals(QuestionnairePublishingMode.UP_TO, roomless.publishingMode)
    Assertions.assertEquals(
        listOf(true, false, true),
        listOf(
            roomless.statisticsPublished,
            roomless.correctOptionsPublished,
            roomless.leaderboardEnabled))
    Assertions.assertSame(roomless.contents.first(), roomless.publishedUpToContent)
    Assertions.assertNull(roomless.activeRound)
    Mockito.verify(questionnaireService).save(roomless)
  }

  @Test
  fun shouldReleaseNothingSeparatelyForASourcePublishingAllContents() {
    val roomless = stored(null)
    val source = stored(ROOM_ID)
    source.publishingMode = QuestionnairePublishingMode.ALL
    templatingService.replaceRoomlessContents(roomless.id!!, source.id!!)
    Assertions.assertNull(roomless.publishedUpToContent)
  }

  @Test
  fun shouldRefuseToTreatRoomQuestionnairesAsTemplates() {
    val room = stored(ROOM_ID)
    val roomless = stored(null)
    Assertions.assertThrows(IllegalArgumentException::class.java) {
      templatingService.replaceRoomlessContents(room.id!!, room.id!!)
    }
    Assertions.assertThrows(RoomlessQuestionnaireException::class.java) {
      templatingService.replaceRoomlessContents(roomless.id!!, stored(null).id!!)
    }
    Assertions.assertThrows(IllegalArgumentException::class.java) {
      templatingService.renameRoomless(room.id!!, NAME)
    }
    Assertions.assertThrows(IllegalArgumentException::class.java) {
      templatingService.deleteRoomless(room.id!!)
    }
    Mockito.verify(questionnaireService, Mockito.never()).delete(anyOf(UUID::class.java))
    Mockito.verify(questionnaireService, Mockito.never()).save(Mockito.any())
  }

  @Test
  fun shouldRenameAndDeleteTemplates() {
    val roomless = stored(null)
    templatingService.renameRoomless(roomless.id!!, NAME)
    Assertions.assertEquals(NAME, roomless.name)
    templatingService.deleteRoomless(roomless.id!!)
    Mockito.verify(questionnaireService).delete(roomless.id!!)
  }

  @Test
  fun shouldTellRoomQuestionnairesFromTemplatesAndUnknownOnes() {
    Assertions.assertTrue(templatingService.isRoomQuestionnaire(stored(ROOM_ID).id!!))
    Assertions.assertFalse(templatingService.isRoomQuestionnaire(stored(null).id!!))
    Assertions.assertFalse(templatingService.isRoomQuestionnaire(UUID.randomUUID()))
  }

  @Test
  fun shouldSummarizeQuestionnairesWithTheirContentCounts() {
    Assertions.assertEquals(
        mapOf<UUID, QuestionnaireSummary>(), templatingService.findSummaries(listOf()))
    Mockito.verifyNoInteractions(contentRepository)

    val withContents = Questionnaire(id = UUID.randomUUID(), type = QuestionnaireType.QUIZ)
    val empty = Questionnaire(id = UUID.randomUUID(), type = QuestionnaireType.SURVEY)
    val ids = listOf(withContents.id!!, empty.id!!)
    Mockito.`when`(contentRepository.countByQuestionnaireIds(ids))
        .thenReturn(listOf(arrayOf<Any>(withContents.id!!, 3L)))
    Mockito.`when`(questionnaireService.findAllById(ids)).thenReturn(listOf(withContents, empty))

    Assertions.assertEquals(
        mapOf(
            withContents.id!! to QuestionnaireSummary(withContents.id!!, "QUIZ", 3),
            empty.id!! to QuestionnaireSummary(empty.id!!, "SURVEY", 0)),
        templatingService.findSummaries(ids))
  }

  @Test
  fun shouldSearchTemplateTextsLiterally() {
    templatingService.findRoomlessIdsMatchingText("50%_off\\")
    Mockito.verify(contentRepository)
        .findRoomlessQuestionnaireIdsByContentTextLike("%50\\%\\_off\\\\%")
  }

  @Test
  fun shouldCopyATemplateIntoARoomRecordingWhereItCameFrom() {
    val templateContents = runtimeStateContents()
    val roomless = stored(null, *templateContents)
    val copy = Questionnaire(id = UUID.randomUUID())
    duplicateInto(copy)
    val template =
        Template(id = roomless.id, license = License(id = "CC-BY-4.0"), attribution = "Particify")

    Assertions.assertSame(copy, templatingService.createRoomCopy(template, ROOM_ID, NAME))

    Assertions.assertEquals(ROOM_ID, copy.roomId)
    Assertions.assertEquals(template.id, copy.sourceTemplateId)
    Assertions.assertEquals(
        templateContents.map { it.id }, copy.contents.map { it.sourceContentId })
    Assertions.assertTrue(copy.contents.all { it.sourceTemplateId == template.id })
    // Recorded per content, so the licence goes along when a content moves elsewhere.
    Assertions.assertTrue(copy.contents.all { it.sourceTemplateLicenseId == "CC-BY-4.0" })
    Assertions.assertTrue(copy.contents.all { it.sourceTemplateAttribution == "Particify" })
    assertRuntimeStateReset(copy.contents)
  }

  @Test
  fun shouldNotCopyATemplateIntoARoomUnderATakenName() {
    val roomless = stored(null)
    Mockito.`when`(questionnaireService.findByRoomIdAndName(ROOM_ID, NAME))
        .thenReturn(Questionnaire())
    Assertions.assertThrows(QuestionnaireWithNameAlreadyExistsException::class.java) {
      templatingService.createRoomCopy(Template(id = roomless.id), ROOM_ID, NAME)
    }
    Mockito.verify(questionnaireService, Mockito.never())
        .duplicate(anyOf(Questionnaire::class.java), Mockito.any(), Mockito.anyString())
  }

  @Test
  fun shouldReportTheContentsCopiedFromATemplate() {
    val templateId = UUID.randomUUID()
    val copied =
        Content(
            id = UUID.randomUUID(),
            sourceTemplateId = templateId,
            sourceTemplateLicenseId = "CC-BY-4.0")
    // Moved in from a copy of another template, whose licence it keeps.
    val otherTemplateId = UUID.randomUUID()
    val moved =
        Content(
            id = UUID.randomUUID(),
            sourceTemplateId = otherTemplateId,
            sourceTemplateLicenseId = "CC-BY-SA-4.0")
    val added = Content(id = UUID.randomUUID())
    val questionnaire = stored(ROOM_ID, copied, moved, added)
    Mockito.`when`(
            contentRepository.findByQuestionnaireIdOrderByPosition(
                questionnaire.id!!, ScrollPosition.offset()))
        .thenReturn(Window.from(listOf(copied, moved, added)) { ScrollPosition.offset() })

    Assertions.assertEquals(
        listOf(copied, moved),
        templatingService.findContentsCopiedFromTemplates(questionnaire.id!!))
    Assertions.assertThrows(QuestionnaireNotFoundException::class.java) {
      templatingService.findContentsCopiedFromTemplates(UUID.randomUUID())
    }
  }
}
