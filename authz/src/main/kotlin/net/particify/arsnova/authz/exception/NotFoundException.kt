package net.particify.arsnova.authz.exception

import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

class NotFoundException : ResponseStatusException {
  constructor() : super(HttpStatus.NOT_FOUND)
  constructor(reason: String?) : super(HttpStatus.NOT_FOUND, reason)
  constructor(reason: String?, cause: Throwable?) : super(HttpStatus.NOT_FOUND, reason, cause)
}
