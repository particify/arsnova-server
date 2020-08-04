package de.thm.arsnova.service.httpgateway.security

import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextImpl
import org.springframework.security.web.server.context.ServerSecurityContextRepository
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@Component
class SecurityContextRepository: ServerSecurityContextRepository {

    override fun save(
        swe: ServerWebExchange?,
        sc: SecurityContext?
    ): Mono<Void?>? {
        throw UnsupportedOperationException("Not supported yet.")
    }

    override fun load(serverWebExchange: ServerWebExchange): Mono<SecurityContext?>? {
        val token = serverWebExchange.request.headers.getFirst("Authorization") ?: "token"
        val authentication: Authentication = AnonymousAuthenticationToken(
            "user-jwt",
            token,
            AuthorityUtils.createAuthorityList("user")
        )

        return Mono.just(SecurityContextImpl(authentication))
    }
}
