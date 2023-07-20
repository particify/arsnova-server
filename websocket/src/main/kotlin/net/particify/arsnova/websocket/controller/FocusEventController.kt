package net.particify.arsnova.websocket.controller

import net.particify.arsnova.websocket.event.FocusEvent
import net.particify.arsnova.websocket.security.JwtTokenUtil
import net.particify.arsnova.websocket.service.FocusEventService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

@RestController
class FocusEventController(
  private val focusEventService: FocusEventService,
  private val jwtTokenUtil: JwtTokenUtil
) {
  companion object {
    const val FOCUS_EVENT_MAPPING = "/room/{roomId}/focus-event"
  }

  @PostMapping(path = [FOCUS_EVENT_MAPPING])
  fun post(
    @PathVariable roomId: String,
    @RequestHeader(name = "Authorization") bearer: String,
    @RequestBody focusEvent: FocusEvent
  ) {
    val roles = jwtTokenUtil.getRolesFromToken(bearer.removePrefix("Bearer "))
    focusEventService.distribute(roles, roomId, focusEvent)
  }

  @GetMapping(path = [FOCUS_EVENT_MAPPING])
  fun get(
    @PathVariable roomId: String
  ): FocusEvent? {
    return focusEventService.getLatestEvent(roomId)
  }
}
