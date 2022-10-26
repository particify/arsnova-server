package de.thm.arsnova.service.authservice.exception

import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

class InternalServerErrorException : ResponseStatusException {
    constructor() : super(HttpStatus.INTERNAL_SERVER_ERROR)
    constructor(reason: String?) : super(HttpStatus.INTERNAL_SERVER_ERROR, reason)
    constructor(reason: String?, cause: Throwable?) : super(HttpStatus.INTERNAL_SERVER_ERROR, reason, cause)
}
