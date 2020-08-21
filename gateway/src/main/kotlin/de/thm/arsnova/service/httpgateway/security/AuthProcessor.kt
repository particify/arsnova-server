package de.thm.arsnova.service.httpgateway.security

import de.thm.arsnova.service.httpgateway.exception.UnauthorizedException
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class AuthProcessor {
    fun getAuthentication(): Mono<Authentication> {
        return ReactiveSecurityContextHolder.getContext()
                .map { securityContext ->
                    securityContext.authentication
                }
                .switchIfEmpty(Mono.error(UnauthorizedException()))
    }
}
