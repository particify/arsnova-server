package net.particify.arsnova.websocket.service

import net.particify.arsnova.websocket.event.FocusEvent
import net.particify.arsnova.websocket.exception.UnauthorizedException
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Service

@Service
class FocusEventService(
  private val rabbitTemplate: RabbitTemplate
) {
  companion object {
    const val OWNER_ROLE_PREFIX = "OWNER-"
    const val EDITOR_ROLE_PREFIX = "EDITOR-"
  }

  private val latestEvents = mutableMapOf<String, FocusEvent>()

  fun distribute(roles: List<String>, roomId: String, focusEvent: FocusEvent) {
    if (roles.contains(OWNER_ROLE_PREFIX + roomId) || roles.contains(EDITOR_ROLE_PREFIX + roomId)) {
      latestEvents[roomId] = focusEvent
      rabbitTemplate.convertAndSend(
        "amq.topic",
        "$roomId.focus.state.stream",
        focusEvent
      )
    } else {
      throw UnauthorizedException()
    }
  }

  fun getLatestEvent(roomId: String): FocusEvent? {
    return latestEvents[roomId]
  }
}
