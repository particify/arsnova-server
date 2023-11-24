package net.particify.arsnova.authz.controller

import net.particify.arsnova.authz.config.AuthServiceProperties
import net.particify.arsnova.authz.exception.BadRequestException
import net.particify.arsnova.authz.exception.ForbiddenException
import net.particify.arsnova.authz.handler.RoomAccessHandler
import net.particify.arsnova.authz.security.JwtUtils
import net.particify.arsnova.common.uuid.UuidHelper
import org.springframework.http.ResponseEntity
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.ResponseBody
import reactor.core.publisher.Mono
import java.util.UUID

@Controller
class JwtController(
  private val roomAccessHandler: RoomAccessHandler,
  private val jwtUtils: JwtUtils,
  authServiceProperties: AuthServiceProperties,
) {
  val uriHeader = authServiceProperties.security.authorizeUriHeader
  val uriPrefix = authServiceProperties.security.authorizeUriPrefix

  @GetMapping("/jwt")
  @ResponseBody
  fun getInternalAuthorization(
    serverHttpRequest: ServerHttpRequest,
    @RequestHeader authorization: String,
  ): Mono<ResponseEntity<Void>> {
    val uri =
      serverHttpRequest.headers[uriHeader]
        ?: throw BadRequestException("$uriHeader header missing.")
    val roomId = extractRoomId(uri.first()) ?: throw BadRequestException("Invalid URI.")
    val encodedJwt = jwtUtils.extractJwtString(authorization) ?: throw BadRequestException("Invalid Authorization header.")
    val publicJwt = jwtUtils.decodeJwt(encodedJwt)
    val userId = UuidHelper.stringToUuid(publicJwt.subject)
    return Mono.justOrEmpty(roomAccessHandler.getByRoomIdAndUserId(roomId, userId!!))
      .switchIfEmpty(Mono.error(ForbiddenException()))
      .flatMap { roomAccess ->
        val internalJwt = jwtUtils.createSignedInternalToken(roomAccess.userId, roomAccess.roomId, roomAccess.role!!)
        Mono.just(ResponseEntity.ok().header("Authorization", "Bearer $internalJwt").build())
      }
  }

  private fun extractRoomId(uri: String): UUID? {
    val path = uri.removePrefix(uriPrefix)
    val roomIdMatch = "^/room/([^/]+)".toRegex().find(path)
    return if (roomIdMatch != null) UuidHelper.stringToUuid(roomIdMatch.groupValues[1]) else null
  }
}
