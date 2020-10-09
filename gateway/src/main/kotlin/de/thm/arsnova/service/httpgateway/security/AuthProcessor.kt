package de.thm.arsnova.service.httpgateway.security

import de.thm.arsnova.service.httpgateway.exception.UnauthorizedException
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class AuthProcessor {
    companion object {
        const val JWT_MONITORING_ROLE_STRING = "MONITORING"
        const val JWT_ADMIN_ROLE_STRING = "ADMIN"
    }

    fun getAuthentication(): Mono<Authentication> {
        return ReactiveSecurityContextHolder.getContext()
                .map { securityContext ->
                    securityContext.authentication
                }
                .switchIfEmpty(Mono.error(UnauthorizedException()))
    }

    fun isAdminOrMonitoring(authentication: Authentication): Boolean {
        return authentication.authorities.toList().any { ga ->
            ga == (SimpleGrantedAuthority(JWT_MONITORING_ROLE_STRING))
                    || ga == (SimpleGrantedAuthority(JWT_ADMIN_ROLE_STRING))
        }
    }
}
