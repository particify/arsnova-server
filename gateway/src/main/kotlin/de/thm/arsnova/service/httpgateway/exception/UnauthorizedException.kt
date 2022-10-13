package de.thm.arsnova.service.httpgateway.exception

import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

class UnauthorizedException : ResponseStatusException {
  constructor() : super(HttpStatus.UNAUTHORIZED)
  constructor(reason: String?) : super(HttpStatus.UNAUTHORIZED, reason)
  constructor(reason: String?, cause: Throwable?) : super(HttpStatus.UNAUTHORIZED, reason, cause)
}
