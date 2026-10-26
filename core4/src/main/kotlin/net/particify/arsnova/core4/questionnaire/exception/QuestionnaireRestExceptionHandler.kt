/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.exception

import java.util.UUID
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/**
 * The REST counterpart of [QuestionnaireExceptionResolverAdapter] for the exceptions the module's
 * REST endpoints raise. Deliberately no ResponseEntityExceptionHandler: only one advice may extend
 * it, and the proprietary build already has one.
 */
@RestControllerAdvice
class QuestionnaireRestExceptionHandler {
  @ExceptionHandler(QuestionnaireNotFoundException::class)
  fun handleQuestionnaireNotFound(ex: QuestionnaireNotFoundException): ProblemDetail =
      notFound(ex.message, ex.id)

  @ExceptionHandler(ContentNotFoundException::class)
  fun handleContentNotFound(ex: ContentNotFoundException): ProblemDetail =
      notFound(ex.message, ex.id)

  @ExceptionHandler(ContentFormatDataInvalidException::class)
  fun handleContentFormatDataInvalid(ex: ContentFormatDataInvalidException): ProblemDetail =
      ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.message)

  private fun notFound(message: String?, id: UUID?): ProblemDetail =
      ProblemDetail.forStatusAndDetail(
          HttpStatus.NOT_FOUND, if (id != null) "$message: $id" else message)
}
