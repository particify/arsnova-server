/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.security

import jakarta.servlet.http.HttpServletRequest
import java.util.UUID
import java.util.regex.Pattern
import net.particify.arsnova.core4.room.MembershipService
import net.particify.arsnova.core4.system.config.SecurityProperties
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtException
import org.springframework.security.oauth2.jwt.JwtValidationException
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.server.ResponseStatusException

@Controller
class JwtController(
    private val jwtUtils: JwtUtils,
    securityProperties: SecurityProperties,
    private val membershipService: MembershipService
) {
  companion object {
    private const val REPLACEMENT_PATTERN: String = "$1-$2-$3-$4-$5"
    private val pattern: Pattern = Pattern.compile("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})")
  }

  val uriHeader = securityProperties.authorizeUriHeader
  val uriPrefix = securityProperties.authorizeUriPrefix

  @GetMapping("/jwt")
  @ResponseBody
  fun getInternalAuthorization(
      request: HttpServletRequest,
      @RequestHeader authorization: String,
  ): ResponseEntity<Unit> {
    val roomId = extractRoomId(request)
    val publicJwt = extractJwt(authorization)
    val userId = stringToUuid(publicJwt.subject)
    val membership =
        membershipService.findOneByRoomIdAndUserId(roomId, userId)
            ?: throw AccessDeniedException("No membership found")
    val internalJwt =
        jwtUtils.createSignedInternalToken(
            membership.user?.id!!, membership.room?.id!!, membership.role!!)
    return ResponseEntity.ok().header("Authorization", "Bearer $internalJwt").build()
  }

  private fun extractJwt(
      authorization: String,
  ): Jwt {
    val encodedJwt =
        jwtUtils.extractJwtString(authorization)
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Authorization header")
    return try {
      jwtUtils.decodeJwt(encodedJwt)
    } catch (e: JwtException) {
      if (e is JwtValidationException) {
        throw BadCredentialsException(e.message, e)
      } else {
        throw ResponseStatusException(HttpStatus.BAD_REQUEST, e.message, e)
      }
    }
  }

  private fun extractRoomId(request: HttpServletRequest): UUID {
    val uri =
        request.getHeader(uriHeader)
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "$uriHeader header missing")
    val path = uri.removePrefix(uriPrefix)
    val roomIdMatch =
        "^/room/([^/]+)".toRegex().find(path)
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid URI")
    return stringToUuid(roomIdMatch.groupValues[1])
  }

  @Suppress("TooGenericExceptionCaught")
  private fun stringToUuid(uuidString: String): UUID {
    try {
      return UUID.fromString(pattern.matcher(uuidString).replaceFirst(REPLACEMENT_PATTERN))
    } catch (e: Exception) {
      throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid UUID", e)
    }
  }
}
