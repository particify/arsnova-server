package de.thm.arsnova.service.httpgateway.security

import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextImpl
import org.springframework.security.web.server.context.ServerSecurityContextRepository
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@Component
class SecurityContextRepository(
        private val jwtTokenUtil: JwtTokenUtil
) : ServerSecurityContextRepository {
    companion object {
        val TOKEN_PREFIX = "Bearer "
    }

    override fun save(
        swe: ServerWebExchange?,
        sc: SecurityContext?
    ): Mono<Void?>? {
        throw UnsupportedOperationException("Not supported yet.")
    }

    override fun load(serverWebExchange: ServerWebExchange): Mono<SecurityContext?>? {
        val authHeader = serverWebExchange.request.headers.getFirst("Authorization")

        return Mono.justOrEmpty(authHeader)
                .filter { potentialHeader: String ->
                    potentialHeader.startsWith(TOKEN_PREFIX)
                }
                .map { authenticationHeader ->
                    authenticationHeader.removePrefix(TOKEN_PREFIX)
                }
                .map { token ->
                    Pair(jwtTokenUtil.getUserIdFromPublicToken(token), token)
                }
                .onErrorResume { Mono.empty() }
                .map { authPair ->
                    UsernamePasswordAuthenticationToken(
                            authPair.first,
                            TOKEN_PREFIX + authPair.second,
                            AuthorityUtils.createAuthorityList("user")
                    )
                }
                .map { authentication ->
                    SecurityContextImpl(authentication)
                }
    }
}
