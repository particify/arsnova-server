/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.content

import jakarta.transaction.Transactional
import java.util.UUID
import net.particify.arsnova.core4.questionnaire.answer.AnswerRepository
import net.particify.arsnova.core4.questionnaire.event.ContentCreatedEvent
import net.particify.arsnova.core4.questionnaire.event.ContentDeletedEvent
import net.particify.arsnova.core4.questionnaire.event.ContentDuplicatedEvent
import net.particify.arsnova.core4.questionnaire.event.ContentsDeletedEvent
import net.particify.arsnova.core4.questionnaire.exception.ContentFormatDataInvalidException
import net.particify.arsnova.core4.questionnaire.exception.ContentNotFoundException
import net.particify.arsnova.core4.questionnaire.exception.QuestionnaireNotFoundException
import net.particify.arsnova.core4.questionnaire.questionnaire.Questionnaire
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnairePublishingMode
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnaireRepository
import net.particify.arsnova.core4.questionnaire.questionnaire.api.QuestionnaireEventPublisher
import net.particify.arsnova.core4.questionnaire.round.Round
import net.particify.arsnova.core4.questionnaire.round.RoundServiceImpl
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.ScrollPosition
import org.springframework.data.domain.Window
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class ContentServiceImpl(
    private val contentRepository: ContentRepository,
    private val questionnaireRepository: QuestionnaireRepository,
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val roundService: RoundServiceImpl,
    private val answerRepository: AnswerRepository,
    private val questionnaireEventPublisher: QuestionnaireEventPublisher
) : ContentRepository by contentRepository {

  fun findByQuestionnaireIdOrderByPositionForCurrentUser(
      questionnaireId: UUID,
      position: ScrollPosition
  ): Window<Content> {
    val window = findByQuestionnaireIdOrderByPosition(questionnaireId, position)

    val questionnaire =
        questionnaireRepository.findByIdOrNull(questionnaireId)
            ?: throw QuestionnaireNotFoundException(questionnaireId)

    return Window.from(
        window.filter { content -> isPublished(content, questionnaire) }.toList(),
        window::positionAt,
    )
  }

  fun isPublished(content: Content, questionnaire: Questionnaire): Boolean {
    return when (questionnaire.publishingMode) {
      QuestionnairePublishingMode.ALL -> true
      QuestionnairePublishingMode.UP_TO,
      QuestionnairePublishingMode.LIVE -> {
        val releasedPosition = questionnaire.publishedUpToContent?.position ?: return false
        content.position!! <= releasedPosition
      }
      null -> false
    }
  }

  @Transactional
  fun create(
      questionnaireId: UUID,
      body: String,
      formatData: ContentFormatData,
      duration: Int?,
      abstentionsAllowed: Boolean?,
      weight: Int?,
  ): Content {
    Content.validateSettings(body, duration, weight)
    formatData.validate()
    val questionnaire =
        questionnaireRepository.findByIdOrNull(questionnaireId)
            ?: throw QuestionnaireNotFoundException(questionnaireId)
    val content =
        Content(
            questionnaire = questionnaire,
            body = body,
            formatData = formatData,
            duration = duration,
            weight = weight ?: 1,
            abstentionsAllowed = abstentionsAllowed ?: true,
            points = if (formatData.hasCorrectAnswer()) Content.calculateBasePoints(weight) else 0)
    questionnaire.contents.add(content)
    releaseFirstContent(questionnaire, content)
    questionnaireRepository.save(questionnaire)
    var persistedContent = questionnaire.contents.last()
    val round = Round.initial(persistedContent, questionnaire.isLive())
    val persistedRound = roundService.save(round)
    persistedContent.activeRound = persistedRound
    persistedContent = contentRepository.save(persistedContent)
    applicationEventPublisher.publishEvent(ContentCreatedEvent(persistedContent.id!!))
    questionnaireEventPublisher.publishQuestionnairesChanged(questionnaire)
    return persistedContent
  }

  @Transactional
  fun update(
      id: UUID,
      body: String,
      formatData: ContentFormatData,
      duration: Int?,
      abstentionsAllowed: Boolean?,
      weight: Int?
  ): Content {
    Content.validateSettings(body, duration, weight)
    val content = contentRepository.findByIdOrNull(id) ?: throw ContentNotFoundException(id)
    val current = content.formatData!!
    val merged = formatData.mergeWith(current)
    merged.validate()
    merged.validateChange(current) { hasAnswers(id) }
    content.body = body
    content.formatData = merged
    content.duration = duration
    content.weight = weight
    content.abstentionsAllowed = abstentionsAllowed ?: true
    // Both the weight and the format data's correctness criteria can change here, so the content's
    // worth is recomputed the same way creation derives it.
    content.points = if (merged.hasCorrectAnswer()) Content.calculateBasePoints(weight) else 0
    return contentRepository.save(content)
  }

  /**
   * Covers every round and hidden answers alike. ContentQueryController.answerCount is not a
   * substitute: it counts the active round only and excludes hidden answers.
   */
  private fun hasAnswers(contentId: UUID): Boolean =
      roundService.findByContentIdOrderByRoundNo(contentId).any {
        answerRepository.existsByRoundId(it.id!!)
      }

  @Transactional
  override fun deleteByQuestionnaireId(questionnaireId: UUID): Int {
    val contents =
        contentRepository
            .findByQuestionnaireIdOrderByPosition(questionnaireId, ScrollPosition.offset())
            .toList()
    contents.forEach { content ->
      content.activeRound = null
      contentRepository.save(content)
      roundService.deleteByContentId(content.id!!)
    }
    applicationEventPublisher.publishEvent(ContentsDeletedEvent(questionnaireId, contents.size))
    return contentRepository.deleteByQuestionnaireId(questionnaireId)
  }

  @Transactional
  fun delete(id: UUID): UUID {
    val content = findByIdOrNull(id) ?: throw ContentNotFoundException(id)
    applicationEventPublisher.publishEvent(ContentDeletedEvent(id))
    val questionnaire = content.questionnaire!!
    releaseReferencesTo(content, questionnaire)
    questionnaire.contents.remove(content)
    contentRepository.deleteById(id)
    questionnaireEventPublisher.publishQuestionnairesChanged(questionnaire)
    return id
  }

  /** A questionnaire which releases contents one by one starts with its first one released. */
  private fun releaseFirstContent(questionnaire: Questionnaire, content: Content) {
    if (questionnaire.publishingMode != QuestionnairePublishingMode.ALL &&
        questionnaire.publishedUpToContent == null) {
      questionnaire.publishedUpToContent = content
    }
  }

  /**
   * Keeps [questionnaire] from referencing [content] once it is no longer part of it. A published
   * range ending at the content shrinks to the preceding one instead of releasing the following
   * one.
   */
  private fun releaseReferencesTo(content: Content, questionnaire: Questionnaire) {
    if (questionnaire.activeRound?.content?.id == content.id) questionnaire.activeRound = null
    if (questionnaire.publishedUpToContent?.id == content.id) {
      val index = questionnaire.contents.indexOfFirst { it.id == content.id }
      questionnaire.publishedUpToContent = questionnaire.contents.getOrNull(index - 1)
    }
  }

  @Transactional
  fun banWordcloudKeyword(id: UUID, keyword: String): Content =
      updateBannedKeywords<WordcloudContentFormatData>(id, "wordcloud") {
        it.addBannedKeyword(keyword)
      }

  @Transactional
  fun resetBannedWordcloudKeywords(id: UUID): Content =
      updateBannedKeywords<WordcloudContentFormatData>(id, "wordcloud") { it.resetBannedKeywords() }

  /**
   * Banned keywords are excluded from the stats of every round of the content, so the rounds'
   * subscribers are told about the change as if an answer had changed.
   */
  private inline fun <reified T : KeywordBanning> updateBannedKeywords(
      id: UUID,
      formatName: String,
      update: (T) -> Unit
  ): Content {
    val content = contentRepository.findByIdOrNull(id) ?: throw ContentNotFoundException(id)
    val formatData = formatDataOf<T>(content, formatName)
    update(formatData)
    content.formatData = formatData
    val persistedContent = contentRepository.save(content)
    roundService.publishRoundsChanged(persistedContent)
    return persistedContent
  }

  private inline fun <reified T : KeywordBanning> formatDataOf(
      content: Content,
      formatName: String
  ): T =
      content.formatData as? T
          ?: throw ContentFormatDataInvalidException("Content is not a $formatName content")

  @Transactional
  fun duplicate(content: Content, questionnaire: Questionnaire? = content.questionnaire): Content {
    val newContent = content.copy()
    newContent.questionnaire = questionnaire
    questionnaire!!.contents.add(newContent)
    val persistedQuestionnaire = questionnaireRepository.save(questionnaire)
    var persistedContent = persistedQuestionnaire.contents.last()
    val round = Round.initial(persistedContent, questionnaire.isLive())
    val persistedRound = roundService.save(round)
    persistedContent.activeRound = persistedRound
    persistedContent = contentRepository.save(persistedContent)
    applicationEventPublisher.publishEvent(
        ContentDuplicatedEvent(content.id!!, persistedContent.id!!))
    questionnaireEventPublisher.publishQuestionnairesChanged(persistedQuestionnaire)
    return persistedContent
  }

  /**
   * The content leaves the source like a deleted one and joins the target like a new one: the
   * source lets go of its pointers to it, and its round takes the state a new content's round has
   * in the target's publishing mode.
   */
  @Transactional
  fun moveContentToQuestionnaire(contentId: UUID, targetQuestionnaireId: UUID): Content {
    val content =
        contentRepository.findByIdOrNull(contentId) ?: throw ContentNotFoundException(contentId)
    val targetQuestionnaire =
        questionnaireRepository.findByIdOrNull(targetQuestionnaireId)
            ?: throw QuestionnaireNotFoundException(targetQuestionnaireId)
    val sourceQuestionnaire = content.questionnaire!!
    if (sourceQuestionnaire.id == targetQuestionnaire.id) {
      return content
    }
    releaseReferencesTo(content, sourceQuestionnaire)
    sourceQuestionnaire.contents.remove(content)
    targetQuestionnaire.contents.add(content)
    content.questionnaire = targetQuestionnaire
    releaseFirstContent(targetQuestionnaire, content)
    alignRoundWith(content, targetQuestionnaire)
    questionnaireRepository.save(sourceQuestionnaire)
    questionnaireRepository.save(targetQuestionnaire)
    val persisted = contentRepository.save(content)
    listOf(sourceQuestionnaire, targetQuestionnaire).forEach {
      questionnaireEventPublisher.publishQuestionnaireUpdated(it.id!!)
      questionnaireEventPublisher.publishQuestionnairesChanged(it)
    }
    return persisted
  }

  private fun alignRoundWith(content: Content, questionnaire: Questionnaire) {
    val round = roundService.findByContentIdOrderByRoundNo(content.id!!).lastOrNull() ?: return
    if (questionnaire.isLive()) {
      round.closeWithoutCountdown()
    } else {
      round.openWithoutCountdown()
    }
    roundService.save(round)
  }

  @Deprecated(
      "Deprecated by base implementation", replaceWith = ReplaceWith("deleteAllInBatch(entities)"))
  override fun deleteInBatch(entities: Iterable<Content>) =
      contentRepository.deleteInBatch(entities)
}
