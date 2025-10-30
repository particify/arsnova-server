/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.security

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.MACSigner
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import java.time.Instant
import java.time.temporal.TemporalAmount
import java.util.Date
import java.util.UUID
import net.particify.arsnova.core4.room.RoomRole
import net.particify.arsnova.core4.system.config.SecurityProperties
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.stereotype.Component

@Component
class JwtUtils(securityProperties: SecurityProperties, private val jwtDecoder: JwtDecoder?) {
  private val signer = MACSigner(securityProperties.jwt.secret)
  private val header = JWSHeader(JWSAlgorithm.HS256)
  private val defaultValidityPeriod: TemporalAmount = securityProperties.jwt.validityPeriod
  private val issuer: String = securityProperties.jwt.issuer

  fun createSignedInternalToken(
      userId: UUID,
      roomId: UUID,
      roomRole: RoomRole,
  ): String {
    val roomIdString = uuidToString(roomId)
    val roomRole = "$roomRole-$roomIdString"
    val roles = listOf(roomRole)
    return encodeJwt(uuidToString(userId), roles)
  }

  fun encodeJwt(
      subject: String,
      roles: List<String>,
      additionalClaims: Map<String, Any> = emptyMap(),
      expirationTime: Instant = Instant.now().plus(defaultValidityPeriod)
  ): String {
    val claims = mapOf("roles" to roles).plus(additionalClaims)
    val jwt = SignedJWT(header, buildClaimsSet(subject, claims, expirationTime))
    jwt.sign(signer)
    return jwt.serialize()
  }

  private fun buildClaimsSet(
      subject: String,
      claims: Map<String, Any>,
      expirationTime: Instant
  ): JWTClaimsSet {
    val issueTime = Instant.now()
    val builder =
        JWTClaimsSet.Builder()
            .issuer(issuer)
            .audience(issuer)
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

  fun uuidToString(uuid: UUID): String {
    return uuid.toString().replace("-", "")
  }
}
