package net.particify.arsnova.authz.security

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.MACSigner
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import net.particify.arsnova.authz.config.AuthServiceProperties
import net.particify.arsnova.common.uuid.UuidHelper
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.temporal.TemporalAmount
import java.util.Date
import java.util.UUID

@Component
class JwtUtils(authServiceProperties: AuthServiceProperties, private val jwtDecoder: JwtDecoder?) {
  private val signer = MACSigner(authServiceProperties.security.jwt.secret)
  private val header = JWSHeader(JWSAlgorithm.HS256)
  private val defaultValidityPeriod: TemporalAmount = authServiceProperties.security.jwt.validityPeriod
  private val serverId: String = authServiceProperties.security.jwt.serverId

  fun createSignedInternalToken(
    userId: UUID,
    roomId: UUID,
    roomRole: String,
  ): String {
    val roomIdString = UuidHelper.uuidToString(roomId)
    val roomRole = "$roomRole-$roomIdString"
    val roles = arrayOf(roomRole)
    return encodeJwt(uuidToString(userId), roles)
  }

  fun encodeJwt(
    subject: String,
    roles: Array<String>,
  ): String {
    val jwt = SignedJWT(header, buildClaimsSet(subject, mapOf("roles" to roles)))
    jwt.sign(signer)
    return jwt.serialize()
  }

  private fun buildClaimsSet(
    subject: String,
    claims: Map<String, Any>,
  ): JWTClaimsSet {
    val issueTime = Instant.now()
    val expirationTime = issueTime.plus(defaultValidityPeriod)

    val builder =
      JWTClaimsSet.Builder()
        .issuer(serverId)
        .audience(serverId)
        .issueTime(Date.from(issueTime))
        .expirationTime(Date.from(expirationTime))
        .subject(subject)

    claims.forEach { (name: String?, value: Any?) ->
      builder.claim(
        name,
        value,
      )
    }

    return builder.build()
  }

  fun decodeJwt(encodedJwt: String): Jwt {
    if (jwtDecoder == null) {
      throw NotImplementedError("JwtDecoder is not available.")
    }
    return jwtDecoder.decode(encodedJwt)
  }

  fun extractJwt(headerValue: String): Jwt? {
    val jwtString = extractJwtString(headerValue)
    return if (jwtString != null) decodeJwt(jwtString) else null
  }

  fun extractJwtString(headerValue: String): String? {
    val jwt =
      "^Bearer (.*)".toRegex(RegexOption.IGNORE_CASE).replace(headerValue) {
        if (it.groupValues.size > 1) it.groupValues[1] else ""
      }
    return jwt.ifEmpty { null }
  }

  private fun uuidToString(uuid: UUID): String {
    return uuid.toString().replace("-", "")
  }
}
