package net.particify.arsnova.authz.controller

import net.particify.arsnova.authz.exception.BadRequestException
import net.particify.arsnova.authz.exception.ForbiddenException
import net.particify.arsnova.authz.handler.RoomAccessHandler
import net.particify.arsnova.authz.model.RoomAccess
import net.particify.arsnova.authz.security.JwtUtils
import net.particify.arsnova.common.uuid.UuidHelper
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.ResponseBody
import reactor.core.publisher.Mono
import java.util.UUID

@Controller
class MembershipController(
  private val handler: RoomAccessHandler,
  private val jwtUtils: JwtUtils,
) {
  @GetMapping("/membership/by-user/{userId}")
  @ResponseBody
  fun listMemberships(
    @PathVariable userId: UUID,
    @RequestHeader authorization: String,
  ): Mono<Iterable<RoomAccess>> {
    val jwt = jwtUtils.extractJwt(authorization) ?: throw BadRequestException()
    val authUserId = UuidHelper.stringToUuid(jwt.subject)
    if (userId != authUserId) {
      throw ForbiddenException()
    }
    return Mono.just(handler.getByUserId(userId))
  }

  @PostMapping("/room/{roomId}/request-membership")
  @ResponseBody
  fun requestMembership(
    @PathVariable roomId: UUID,
    @RequestHeader authorization: String,
  ): Mono<RoomAccess> {
    val jwt = jwtUtils.extractJwt(authorization) ?: throw BadRequestException()
    val userId = UuidHelper.stringToUuid(jwt.subject)
    return Mono.just(handler.create(RoomAccess(roomId, userId)))
  }

  @PostMapping("/room/{roomId}/cancel-membership")
  fun cancelMembership(
    @PathVariable roomId: UUID,
    @RequestHeader authorization: String,
  ): Mono<Unit> {
    val jwt = jwtUtils.extractJwt(authorization) ?: throw BadRequestException()
    val userId = UuidHelper.stringToUuid(jwt.subject)
    return Mono.just(handler.delete(roomId, userId))
  }
}
