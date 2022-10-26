package de.thm.arsnova.service.httpgateway.security

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.exceptions.TokenExpiredException
import de.thm.arsnova.service.httpgateway.config.HttpGatewayProperties
import de.thm.arsnova.service.httpgateway.exception.UnauthorizedException
import de.thm.arsnova.service.httpgateway.model.RoomAccess
import de.thm.arsnova.service.httpgateway.model.RoomFeatures
import org.springframework.security.core.authority.SimpleGrantedAuthority
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
        const val ROLE_AUTHORITY_PREFIX = "ROLE_"
        val rolesClaimName = "roles"
        val roomFeaturesClaimName = "features"
        val tierIdClaimName = "tierId"
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

    fun getAuthoritiesFromPublicToken(token: String): List<SimpleGrantedAuthority> {
        try {
            val decodedJwt = publicVerifier.verify(token)
            return decodedJwt.getClaim("roles").asList(String::class.java).map { role ->
                SimpleGrantedAuthority(ROLE_AUTHORITY_PREFIX + role)
            }
        } catch (e: JWTVerificationException) {
            throw UnauthorizedException()
        } catch (e: TokenExpiredException) {
            throw UnauthorizedException()
        }
    }

    fun getUserIdAndClientRolesFromPublicToken(token: String): Pair<String, List<String>> {
        try {
            val decodedJwt = publicVerifier.verify(token)
            val authorities = decodedJwt.getClaim("roles").asList(String::class.java)
            return Pair(decodedJwt.subject, authorities)
        } catch (e: JWTVerificationException) {
            throw UnauthorizedException()
        } catch (e: TokenExpiredException) {
            throw UnauthorizedException()
        }
    }

    fun createSignedInternalToken(
            roomAccess: RoomAccess,
            roomFeatures: RoomFeatures,
            clientAuthorities: List<String>
    ): String {
        val roomRole = "${roomAccess.role}-${roomAccess.roomId}"
        val generatedRoles = arrayOf(roomRole)
        val roles = generatedRoles + clientAuthorities
        val roomFeaturesArray = roomFeatures.features.toTypedArray()
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
            .withArrayClaim(rolesClaimName, roles)
            .withArrayClaim(roomFeaturesClaimName, roomFeaturesArray)
            .withClaim(tierIdClaimName, roomFeatures.tierId)
            .sign(internalAlgorithm)
    }
}
