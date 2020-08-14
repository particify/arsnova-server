package de.thm.arsnova.service.wsgateway.security

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.interfaces.DecodedJWT
import de.thm.arsnova.service.wsgateway.config.WebSocketProperties
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
class JwtTokenUtil(
		private val webSocketProperties: WebSocketProperties
) {
	private val algorithm = Algorithm.HMAC256(webSocketProperties.security.jwt.secret)
	private val verifier: JWTVerifier = JWT.require(algorithm).build()

	@Throws(JWTVerificationException::class)
	fun getUserId(token: String): String {
		val decodedJwt = verifier.verify(token)
		return decodedJwt.subject
	}
}
