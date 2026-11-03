/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.security.internal

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.MACSigner
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import java.text.ParseException
import java.time.Instant
import java.util.Date
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import net.particify.arsnova.core4.security.SecurityProperties
import org.springframework.security.oauth2.jwt.BadJwtException
import org.springframework.security.oauth2.jwt.JwtTimestampValidator
import org.springframework.security.oauth2.jwt.JwtValidators
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.stereotype.Component

const val CHALLENGE_SOLVED_REUSABLE_ROLE = "CHALLENGE_SOLVED_REUSABLE"

private const val SUBJECT = "reusable-challenge-token"
private const val KEY_LABEL = "reusable-challenge-token"
private const val KEY_ALGORITHM = "HmacSHA256"
private const val ROLES_CLAIM = "roles"

/**
 * Issues and verifies the tokens a solved challenge can be exchanged for instead of a single-use
 * one.
 *
 * They are signed with a key derived from the JWT secret for reusable tokens alone. Every other
 * verifier of the JWT secret, in this service and in others, thereby rejects them, so they can
 * never stand in for a session. The subject is not an account ID for the same reason.
 */
@Component
class ReusableChallengeTokens(securityProperties: SecurityProperties) {
  private val validity = securityProperties.challenge.reusableTokenValidity
  private val key = deriveKey(securityProperties.jwt.secret)
  private val signer = MACSigner(key)
  private val issuer = securityProperties.jwt.issuer
  private val decoder =
      NimbusJwtDecoder.withSecretKey(SecretKeySpec(key, KEY_ALGORITHM)).build().apply {
        setJwtValidator(
            JwtValidators.createDefaultWithValidators(JwtTimestampValidator(JWT_CLOCK_SKEW)))
      }

  fun issue(challengeId: UUID, issueTime: Instant = Instant.now()): String {
    val claims =
        JWTClaimsSet.Builder()
            .issuer(issuer)
            .audience(issuer)
            .subject(SUBJECT)
            .jwtID(challengeId.toString())
            .issueTime(Date.from(issueTime))
            .expirationTime(Date.from(issueTime.plus(validity)))
            .claim(ROLES_CLAIM, listOf(CHALLENGE_SOLVED_REUSABLE_ROLE))
            .build()
    val jwt = SignedJWT(JWSHeader(JWSAlgorithm.HS256), claims)
    jwt.sign(signer)
    return jwt.serialize()
  }

  /**
   * Whether the token claims to be one of these, judged without verifying it. Other tokens are left
   * to the filters for them rather than failing verification here.
   */
  fun isClaimedBy(token: String): Boolean =
      try {
        SignedJWT.parse(token).jwtClaimsSet.subject == SUBJECT
      } catch (_: ParseException) {
        false
      }

  /** Throws a `JwtException` for a token which is not a valid one of these. */
  fun verify(token: String) {
    val jwt = decoder.decode(token)
    if (jwt.subject != SUBJECT) {
      throw BadJwtException("Not a reusable challenge token")
    }
  }

  private fun deriveKey(secret: String): ByteArray {
    val mac = Mac.getInstance(KEY_ALGORITHM)
    mac.init(SecretKeySpec(secret.toByteArray(), KEY_ALGORITHM))
    return mac.doFinal(KEY_LABEL.toByteArray())
  }
}
