package de.thm.arsnova.service.authservice.exception

import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

class BadRequestException : ResponseStatusException {
  constructor() : super(HttpStatus.BAD_REQUEST)
  constructor(reason: String?) : super(HttpStatus.BAD_REQUEST, reason)
  constructor(reason: String?, cause: Throwable?) : super(HttpStatus.BAD_REQUEST, reason, cause)
}
