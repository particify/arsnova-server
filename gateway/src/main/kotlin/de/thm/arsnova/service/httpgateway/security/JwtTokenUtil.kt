package de.thm.arsnova.service.httpgateway.security

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.exceptions.TokenExpiredException
import de.thm.arsnova.service.httpgateway.config.HttpGatewayProperties
import de.thm.arsnova.service.httpgateway.exception.UnauthorizedException
import de.thm.arsnova.service.httpgateway.model.RoomAccess
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.TemporalAmount
import java.util.Date

@Component
class JwtTokenUtil(
        private val httpGatewayProperties: HttpGatewayProperties
) {
    companion object {
        val claimName = "roles"
    }

    private val publicAlgorithm = Algorithm.HMAC256(httpGatewayProperties.security.jwt.publicSecret)
    private val internalAlgorithm = Algorithm.HMAC256(httpGatewayProperties.security.jwt.internalSecret)
    private val defaultValidityPeriod: TemporalAmount = httpGatewayProperties.security.jwt.validityPeriod
    private val serverId: String = httpGatewayProperties.security.jwt.serverId
    private val publicVerifier: JWTVerifier = JWT.require(publicAlgorithm).build()
    private val internalVerifier: JWTVerifier = JWT.require(internalAlgorithm).build()

    fun getUserIdFromPublicToken(token: String): String {
        try {
            val decodedJwt = publicVerifier.verify(token)
            return decodedJwt.subject
        } catch (e: JWTVerificationException) {
            throw UnauthorizedException()
        } catch (e: TokenExpiredException) {
            throw UnauthorizedException()
        }
    }

    fun createSignedInternalToken(roomAccess: RoomAccess): String {
        val roles = arrayOf("${roomAccess.role}-${roomAccess.roomId}")
        return JWT.create()
            .withIssuer(serverId)
            .withAudience(serverId)
            .withIssuedAt(Date())
            .withExpiresAt(
                Date.from(
                    LocalDateTime.now().plus(defaultValidityPeriod).atZone(ZoneId.systemDefault()).toInstant()
                )
            )
            .withSubject(roomAccess.userId)
            .withArrayClaim(claimName, roles)
            .sign(internalAlgorithm)
    }
}
