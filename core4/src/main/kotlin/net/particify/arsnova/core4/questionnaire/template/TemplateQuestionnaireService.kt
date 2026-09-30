/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.template

import jakarta.transaction.Transactional
import java.util.UUID
import net.particify.arsnova.core4.questionnaire.content.Content
import net.particify.arsnova.core4.questionnaire.content.ContentRepository
import net.particify.arsnova.core4.questionnaire.content.ContentServiceImpl
import net.particify.arsnova.core4.questionnaire.exception.QuestionnaireNotFoundException
import net.particify.arsnova.core4.questionnaire.exception.QuestionnaireWithNameAlreadyExistsException
import net.particify.arsnova.core4.questionnaire.questionnaire.Questionnaire
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnairePublishingMode
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnaireServiceImpl
import org.springframework.data.domain.ScrollPosition
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

data class QuestionnaireSummary(val id: UUID, val type: String, val contentCount: Int)

/**
 * Keeps a template's contents in a room-less questionnaire, which shares the template's id. Copies
 * into or out of a room-less questionnaire drop the state a presentation accumulates, such as
 * banned keywords or published text answers.
 */
@Service
class TemplateQuestionnaireService(
    private val questionnaireServiceImpl: QuestionnaireServiceImpl,
    private val contentServiceImpl: ContentServiceImpl,
    private val contentRepository: ContentRepository,
) {

  /** Copies a room's questionnaire into a new room-less one and returns the new one's id. */
  @Transactional
  fun createRoomlessCopy(questionnaireId: UUID, name: String): UUID {
    val original = findQuestionnaire(questionnaireId)
    original.requireRoomId()
    val copy = questionnaireServiceImpl.duplicate(original, null, name)
    clearProvenance(copy)
    copy.contents.forEach(::resetRuntimeState)
    // Flushed because the template row's foreign key to this questionnaire is no JPA association,
    // so Hibernate's insert ordering does not know to insert the questionnaire first.
    return questionnaireServiceImpl.saveAndFlush(copy).id!!
  }

  /** Replaces the contents and settings of a room-less questionnaire with those of another. */
  @Transactional
  fun replaceRoomlessContents(roomlessId: UUID, sourceQuestionnaireId: UUID) {
    val roomless = findRoomless(roomlessId)
    val source = findQuestionnaire(sourceQuestionnaireId)
    source.requireRoomId()
    roomless.publishedUpToContent = null
    roomless.activeRound = null
    roomless.contents.mapNotNull { it.id }.forEach { contentServiceImpl.delete(it) }
    roomless.publishingMode = source.publishingMode
    roomless.statisticsPublished = source.statisticsPublished
    roomless.correctOptionsPublished = source.correctOptionsPublished
    roomless.leaderboardEnabled = source.leaderboardEnabled
    clearProvenance(roomless)
    source.contents.toList().forEach {
      resetRuntimeState(contentServiceImpl.duplicate(it, roomless))
    }
    if (roomless.publishingMode != QuestionnairePublishingMode.ALL) {
      roomless.publishedUpToContent = roomless.contents.firstOrNull()
    }
    questionnaireServiceImpl.save(roomless)
  }

  @Transactional
  fun renameRoomless(id: UUID, name: String) {
    val roomless = findRoomless(id)
    roomless.name = name
    questionnaireServiceImpl.save(roomless)
  }

  /** Deletes a room-less questionnaire with its contents. */
  @Transactional
  fun deleteRoomless(id: UUID) {
    findRoomless(id)
    questionnaireServiceImpl.delete(id)
  }

  /** Whether the questionnaire exists and belongs to a room. */
  fun isRoomQuestionnaire(id: UUID): Boolean =
      questionnaireServiceImpl.findByIdOrNull(id)?.isRoomless() == false

  /** Those of [ids] which exist and belong to a room. */
  fun findRoomQuestionnaireIds(ids: Collection<UUID>): Set<UUID> {
    if (ids.isEmpty()) return emptySet()
    return questionnaireServiceImpl
        .findAllById(ids)
        .filterNot { it.isRoomless() }
        .mapNotNull { it.id }
        .toSet()
  }

  fun findSummaries(ids: Collection<UUID>): Map<UUID, QuestionnaireSummary> {
    if (ids.isEmpty()) return emptyMap()
    val counts =
        contentRepository.countByQuestionnaireIds(ids).associate {
          it[0] as UUID to (it[1] as Number).toInt()
        }
    return questionnaireServiceImpl.findAllById(ids).associate {
      it.id!! to QuestionnaireSummary(it.id!!, it.type!!.name, counts[it.id] ?: 0)
    }
  }

  /** Room-less questionnaires with a content whose body or answer texts contain [search]. */
  fun findRoomlessIdsMatchingText(search: String): Set<UUID> =
      contentRepository
          .findRoomlessQuestionnaireIdsByContentTextLike("%${escapeLikePattern(search)}%")
          .toSet()

  /**
   * Copies the template's room-less questionnaire into [roomId] as an unpublished questionnaire
   * which records the template it came from.
   */
  @Transactional
  fun createRoomCopy(template: Template, roomId: UUID, name: String): Questionnaire {
    val roomless = findRoomless(template.id!!)
    if (questionnaireServiceImpl.findByRoomIdAndName(roomId, name) != null) {
      throw QuestionnaireWithNameAlreadyExistsException()
    }
    val copy = questionnaireServiceImpl.duplicate(roomless, roomId, name)
    copy.sourceTemplateId = template.id
    copy.sourceTemplateLicenseId = template.license?.id
    copy.sourceTemplateAttribution = template.attribution
    copy.contents.zip(roomless.contents).forEach { (copiedContent, templateContent) ->
      resetRuntimeState(copiedContent)
      copiedContent.sourceTemplateId = template.id
      copiedContent.sourceContentId = templateContent.id
      copiedContent.sourceTemplateLicenseId = template.license?.id
      contentRepository.save(copiedContent)
    }
    return questionnaireServiceImpl.save(copy)
  }

  /**
   * The contents a room's questionnaire has from templates, with the provenance recorded when they
   * were copied, which outlives the template itself. A questionnaire never copied from a template
   * has none.
   */
  @Transactional
  fun findContentsCopiedFromTemplates(questionnaireId: UUID): List<Content> {
    findQuestionnaire(questionnaireId)
    return contentRepository
        .findByQuestionnaireIdOrderByPosition(questionnaireId, ScrollPosition.offset())
        .filter { it.sourceTemplateId != null }
        .toList()
  }

  /**
   * Template provenance counts as runtime state as well: it records which template a room's
   * questionnaire was copied from, so a room-less questionnaire, which holds a template's own
   * contents, never carries any, and a room copy only carries the provenance [createRoomCopy] sets.
   */
  private fun resetRuntimeState(content: Content) {
    content.formatData = content.formatData?.withoutRuntimeState()
    content.sourceTemplateId = null
    content.sourceContentId = null
    content.sourceTemplateLicenseId = null
    contentRepository.save(content)
  }

  private fun clearProvenance(questionnaire: Questionnaire) {
    questionnaire.sourceTemplateId = null
    questionnaire.sourceTemplateLicenseId = null
    questionnaire.sourceTemplateAttribution = null
  }

  private fun findQuestionnaire(id: UUID): Questionnaire =
      questionnaireServiceImpl.findByIdOrNull(id) ?: throw QuestionnaireNotFoundException(id)

  private fun findRoomless(id: UUID): Questionnaire {
    val questionnaire = findQuestionnaire(id)
    require(questionnaire.isRoomless()) { "Questionnaire $id belongs to a room" }
    return questionnaire
  }
}

/** Makes `%`, `_` and the escape character itself match literally in an ILIKE pattern. */
private fun escapeLikePattern(text: String): String =
    text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
