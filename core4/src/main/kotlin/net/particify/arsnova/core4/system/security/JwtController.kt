/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.security

import jakarta.servlet.http.HttpServletRequest
import java.util.UUID
import java.util.regex.Pattern
import net.particify.arsnova.core4.room.MembershipService
import net.particify.arsnova.core4.system.config.SecurityProperties
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.ResponseBody

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
  ): ResponseEntity<Void> {
    val uri = request.getHeader(uriHeader) ?: error("$uriHeader header missing.")
    val roomId = extractRoomId(uri) ?: error("Invalid URI.")
    val encodedJwt =
        jwtUtils.extractJwtString(authorization) ?: error("Invalid Authorization header.")
    val publicJwt = jwtUtils.decodeJwt(encodedJwt)
    val userId = stringToUuid(publicJwt.subject)
    val membership =
        membershipService.findOneByRoomIdAndUserId(roomId, userId!!)
            ?: error("No membership found.")
    val internalJwt =
        jwtUtils.createSignedInternalToken(
            membership.user?.id!!, membership.room?.id!!, membership.role!!)
    return ResponseEntity.ok().header("Authorization", "Bearer $internalJwt").build()
  }

  private fun extractRoomId(uri: String): UUID? {
    val path = uri.removePrefix(uriPrefix)
    val roomIdMatch = "^/room/([^/]+)".toRegex().find(path)
    return if (roomIdMatch != null) stringToUuid(roomIdMatch.groupValues[1]) else null
  }

  private fun stringToUuid(uuidString: String?): UUID? {
    if (uuidString.isNullOrEmpty()) {
      return null
    }
    return UUID.fromString(pattern.matcher(uuidString).replaceFirst(REPLACEMENT_PATTERN))
  }
}
