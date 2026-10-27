/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.common.exception

import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/** The REST counterpart of [CommonExceptionResolverAdapter] for the exceptions REST can raise. */
@RestControllerAdvice
class CommonRestExceptionHandler {
  @ExceptionHandler(InvalidInputException::class)
  fun handleInvalidInput(ex: InvalidInputException): ProblemDetail =
      ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.message)
}
