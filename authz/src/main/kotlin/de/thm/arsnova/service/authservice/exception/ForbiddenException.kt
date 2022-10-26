package de.thm.arsnova.service.authservice.exception

import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

class ForbiddenException : ResponseStatusException {
    constructor() : super(HttpStatus.FORBIDDEN)
    constructor(reason: String?) : super(HttpStatus.FORBIDDEN, reason)
    constructor(reason: String?, cause: Throwable?) : super(HttpStatus.FORBIDDEN, reason, cause)
}
