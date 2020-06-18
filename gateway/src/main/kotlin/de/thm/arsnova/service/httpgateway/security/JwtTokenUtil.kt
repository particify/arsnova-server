package de.thm.arsnova.service.httpgateway.security

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import de.thm.arsnova.service.httpgateway.config.HttpGatewayProperties
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

    private val algorithm = Algorithm.HMAC256(httpGatewayProperties.security?.jwt?.publicSecret!!)
    private val interServiceAlgorithm = Algorithm.HMAC256(httpGatewayProperties.security?.jwt?.internalSecret!!)
    private val defaultValidityPeriod: TemporalAmount = httpGatewayProperties.security?.jwt?.validityPeriod!!
    private val serverId: String = httpGatewayProperties.security?.jwt?.serverId!!
    private val verifier: JWTVerifier = JWT.require(algorithm).build()

    @Throws(JWTVerificationException::class)
    fun getUserId(token: String): String {
        val decodedJwt = verifier.verify(token)
        return decodedJwt.subject
    }

    fun createSignedToken(roomAccess: RoomAccess): String {
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
            .sign(interServiceAlgorithm)
    }
}
