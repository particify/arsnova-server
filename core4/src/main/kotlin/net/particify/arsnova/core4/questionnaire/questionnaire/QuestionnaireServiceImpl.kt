/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.questionnaire

import jakarta.transaction.Transactional
import java.util.UUID
import kotlin.collections.indexOf
import net.particify.arsnova.core4.questionnaire.content.Content
import net.particify.arsnova.core4.questionnaire.content.ContentRepository
import net.particify.arsnova.core4.questionnaire.content.ContentServiceImpl
import net.particify.arsnova.core4.questionnaire.event.QuestionnaireCreatedEvent
import net.particify.arsnova.core4.questionnaire.event.QuestionnaireDeletedEvent
import net.particify.arsnova.core4.questionnaire.event.QuestionnaireDuplicatedEvent
import net.particify.arsnova.core4.questionnaire.exception.QuestionnaireNotFoundException
import net.particify.arsnova.core4.questionnaire.exception.QuestionnaireWithNameAlreadyExistsException
import net.particify.arsnova.core4.questionnaire.questionnaire.api.QuestionnaireEventPublisher
import net.particify.arsnova.core4.questionnaire.round.RoundServiceImpl
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.ScrollPosition
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class QuestionnaireServiceImpl(
    private val questionnaireRepository: QuestionnaireRepository,
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val contentRepository: ContentRepository,
    private val roundServiceImpl: RoundServiceImpl,
    private val questionnaireEventPublisher: QuestionnaireEventPublisher,
    private val contentServiceImpl: ContentServiceImpl
) : QuestionnaireRepository by questionnaireRepository {
  @Transactional
  fun create(roomId: UUID, name: String, type: QuestionnaireType): Questionnaire {
    if (questionnaireRepository.findByRoomIdAndName(roomId, name) != null) {
      throw QuestionnaireWithNameAlreadyExistsException()
    }
    val publishingMode =
        if (type == QuestionnaireType.QUIZ) QuestionnairePublishingMode.LIVE
        else QuestionnairePublishingMode.ALL
    val questionnaire =
        Questionnaire(roomId = roomId, name = name, type = type, publishingMode = publishingMode)
    if (type != QuestionnaireType.FLASHCARDS) {
      questionnaire.statisticsPublished = true
      if (type != QuestionnaireType.SURVEY) {
        questionnaire.correctOptionsPublished = true
        if (type == QuestionnaireType.QUIZ) {
          questionnaire.leaderboardEnabled = true
        }
      }
    }
    val persistedQuestionnaire = questionnaireRepository.save(questionnaire)
    applicationEventPublisher.publishEvent(QuestionnaireCreatedEvent(persistedQuestionnaire.id!!))
    questionnaireEventPublisher.publishQuestionnairesChanged(persistedQuestionnaire)
    return persistedQuestionnaire
  }

  @Transactional
  fun updatePublished(id: UUID, published: Boolean): Questionnaire {
    val questionnaire = findByIdOrNull(id) ?: throw QuestionnaireNotFoundException(id)
    questionnaire.published = published
    val persisted = questionnaireRepository.save(questionnaire)
    questionnaireEventPublisher.publishQuestionnaireUpdated(id)
    questionnaireEventPublisher.publishQuestionnairesChanged(persisted)
    return persisted
  }

  @Transactional
  fun update(
      id: UUID,
      name: String?,
      published: Boolean?,
      publishingMode: QuestionnairePublishingMode?,
      statisticsPublished: Boolean?,
      correctOptionsPublished: Boolean?,
      leaderboardEnabled: Boolean?
  ): Questionnaire {
    val questionnaire = findByIdOrNull(id) ?: throw QuestionnaireNotFoundException(id)
    if (name != null && name != questionnaire.name) {
      if (questionnaireRepository.findByRoomIdAndName(questionnaire.roomId!!, name) != null) {
        throw QuestionnaireWithNameAlreadyExistsException()
      }
      questionnaire.name = name
    }
    if (published != null) {
      questionnaire.published = published
    }
    if (publishingMode != null && publishingMode != questionnaire.publishingMode) {
      changePublishingMode(questionnaire, publishingMode)
    }
    if (statisticsPublished != null) {
      questionnaire.statisticsPublished = statisticsPublished
    }
    if (correctOptionsPublished != null) {
      questionnaire.correctOptionsPublished = correctOptionsPublished
    }
    if (leaderboardEnabled != null) {
      questionnaire.leaderboardEnabled = leaderboardEnabled
    }
    val persisted = questionnaireRepository.save(questionnaire)
    questionnaireEventPublisher.publishQuestionnaireUpdated(id)
    questionnaireEventPublisher.publishQuestionnairesChanged(persisted)
    return persisted
  }

  /**
   * Only acts on an actual change, so resending the current mode does not rewind the published
   * range. Rounds created in LIVE mode start closed, so leaving it opens every content's round.
   */
  private fun changePublishingMode(
      questionnaire: Questionnaire,
      publishingMode: QuestionnairePublishingMode
  ) {
    val previousMode = questionnaire.publishingMode
    questionnaire.publishingMode = publishingMode
    if (publishingMode != QuestionnairePublishingMode.ALL) {
      questionnaire.publishedUpToContent = questionnaire.contents.firstOrNull()
    }
    if (previousMode == QuestionnairePublishingMode.LIVE) {
      questionnaire.contents.forEach { it.activeRound?.openWithoutCountdown() }
    }
  }

  @Transactional
  fun delete(id: UUID): UUID {
    val questionnaire = findByIdOrNull(id)
    // Its contents and rounds are deleted first, so it must not keep referencing them.
    questionnaire?.activeRound = null
    questionnaire?.publishedUpToContent = null
    applicationEventPublisher.publishEvent(QuestionnaireDeletedEvent(id))
    questionnaireRepository.deleteById(id)
    questionnaire?.let { questionnaireEventPublisher.publishQuestionnairesChanged(it) }
    return id
  }

  /** Deletes every questionnaire of the room together with its contents, rounds and answers. */
  @Transactional
  fun deleteByRoomId(roomId: UUID) {
    questionnaireRepository.findByRoomIdOrderByName(roomId).mapNotNull { it.id }.forEach(::delete)
  }

  /** Copies every questionnaire of a room into the room's duplicate. */
  @Transactional
  fun duplicateForRoom(originalRoomId: UUID, duplicatedRoomId: UUID) {
    questionnaireRepository.findByRoomIdOrderByName(originalRoomId).forEach {
      duplicate(it, duplicatedRoomId, it.name)
    }
  }

  /** Copies a questionnaire within its room under [newName]. */
  @Transactional
  fun duplicate(id: UUID, newName: String): Questionnaire {
    val original = findByIdOrNull(id) ?: throw QuestionnaireNotFoundException(id)
    val roomId = checkNotNull(original.roomId)
    if (questionnaireRepository.findByRoomIdAndName(roomId, newName) != null) {
      throw QuestionnaireWithNameAlreadyExistsException()
    }
    return duplicate(original, roomId, newName)
  }

  /**
   * Copies [original] into [roomId] with its settings and its contents in order. Rounds and answers
   * are not copied: every content starts over with a fresh round.
   */
  @Transactional
  fun duplicate(original: Questionnaire, roomId: UUID, name: String?): Questionnaire {
    val copy =
        questionnaireRepository.save(
            Questionnaire(
                roomId = roomId,
                name = name,
                type = original.type,
                published = original.published,
                publishingMode = original.publishingMode,
                statisticsPublished = original.statisticsPublished,
                correctOptionsPublished = original.correctOptionsPublished,
                leaderboardEnabled = original.leaderboardEnabled))
    original.contents.toList().forEach { contentServiceImpl.duplicate(it, copy) }
    copy.publishedUpToContent = copiedPublishedUpToContent(original, copy)
    val persisted = questionnaireRepository.save(copy)
    applicationEventPublisher.publishEvent(
        QuestionnaireDuplicatedEvent(checkNotNull(original.id), checkNotNull(persisted.id)))
    questionnaireEventPublisher.publishQuestionnairesChanged(persisted)
    return persisted
  }

  /**
   * A copy starts over at its first content, as after resetting its results, because how far a
   * series was released belongs to its presentation rather than its settings.
   */
  private fun copiedPublishedUpToContent(original: Questionnaire, copy: Questionnaire): Content? =
      if (original.publishingMode == QuestionnairePublishingMode.ALL) null
      else copy.contents.firstOrNull()

  @Transactional
  fun updateContentOrder(questionnaireId: UUID, contentIds: List<UUID>): Questionnaire {
    val questionnaire =
        findByIdOrNull(questionnaireId) ?: throw QuestionnaireNotFoundException(questionnaireId)
    // Contents missing from the list keep their relative order after the listed ones.
    questionnaire.contents.sortWith(
        compareBy { contentIds.indexOf(it.id).takeIf { index -> index >= 0 } ?: Int.MAX_VALUE })
    val persisted = questionnaireRepository.save(questionnaire)
    questionnaireEventPublisher.publishQuestionnaireUpdated(questionnaireId)
    return persisted
  }

  @Transactional
  fun resetAnswerResultsByQuestionnaireId(id: UUID): Boolean {
    val questionnaire =
        questionnaireRepository.findByIdOrNull(id) ?: throw QuestionnaireNotFoundException(id)
    questionnaire.activeRound = null
    val contents =
        contentRepository.findByQuestionnaireIdOrderByPosition(id, ScrollPosition.offset())
    questionnaire.publishedUpToContent = contents.firstOrNull()
    questionnaireRepository.save(questionnaire)
    contents.forEach { roundServiceImpl.resetRounds(it) }
    questionnaireEventPublisher.publishQuestionnaireUpdated(id)
    return true
  }

  @Deprecated(
      "Deprecated by base implementation", replaceWith = ReplaceWith("deleteAllInBatch(entities)"))
  override fun deleteInBatch(entities: Iterable<Questionnaire>) =
      questionnaireRepository.deleteInBatch(entities)
}
