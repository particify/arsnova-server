/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.questionnaire.api

import java.util.UUID
import net.particify.arsnova.core4.questionnaire.content.ContentServiceImpl
import net.particify.arsnova.core4.questionnaire.exception.QuestionnaireNotFoundException
import net.particify.arsnova.core4.questionnaire.questionnaire.Questionnaire
import net.particify.arsnova.core4.questionnaire.questionnaire.QuestionnaireServiceImpl
import org.springframework.data.repository.findByIdOrNull
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

@Controller
@PreAuthorize("hasRole('USER')")
@SchemaMapping(typeName = "Query")
class QuestionnaireQueryController(
    private val questionnaireServiceImpl: QuestionnaireServiceImpl,
    private val contentServiceImpl: ContentServiceImpl
) {
  @QueryMapping
  @PreAuthorize("hasPermission(#roomId, 'Room', 'moderate')")
  fun questionnairesByRoomId(@Argument roomId: UUID): List<Questionnaire> {
    return questionnaireServiceImpl.findByRoomIdOrderByName(roomId)
  }

  @QueryMapping
  @PreAuthorize("hasPermission(#roomId, 'Room', 'read')")
  fun publishedQuestionnairesByRoomId(@Argument roomId: UUID): List<Questionnaire> {
    return questionnaireServiceImpl.findPublishedByRoomIdOrderByName(roomId)
  }

  @QueryMapping
  @PreAuthorize("hasPermission(#id, 'Questionnaire', 'read')")
  fun questionnaireById(@Argument id: UUID): Questionnaire {
    return questionnaireServiceImpl.findByIdOrNull(id) ?: throw QuestionnaireNotFoundException(id)
  }

  @SchemaMapping(typeName = "Questionnaire", field = "contentCount")
  fun contentCount(questionnaire: Questionnaire): Int {
    return contentServiceImpl.countByQuestionnaireId(questionnaire.id!!)
  }
}
