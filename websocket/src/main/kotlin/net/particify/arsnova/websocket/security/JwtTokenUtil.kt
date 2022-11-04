package net.particify.arsnova.websocket.security

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import net.particify.arsnova.websocket.config.WebSocketProperties
import org.springframework.stereotype.Component

@Component
class JwtTokenUtil(
  private val webSocketProperties: WebSocketProperties
) {
  private val algorithm = Algorithm.HMAC256(webSocketProperties.security.jwt.secret)
  private val verifier: JWTVerifier = JWT.require(algorithm).build()

  @Throws(JWTVerificationException::class)
  fun getUser(token: String): User {
    val decodedJwt = verifier.verify(token)
    return User(decodedJwt.subject)
  }
}