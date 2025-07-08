package net.particify.arsnova.websocket.handler

import org.slf4j.LoggerFactory
import org.springframework.messaging.simp.user.SimpUserRegistry
import org.springframework.stereotype.Component

@Component
class StompMetaHandler(
  private val simpUserRegistry: SimpUserRegistry,
) {
  private val logger = LoggerFactory.getLogger(StompMetaHandler::class.java)

  fun handleUserSubscribed(roomId: String) {
    logger.error("/topic/$roomId.comment.stream")
    logger.error(
      simpUserRegistry
        .findSubscriptions {
          it.destination == "/topic/$roomId.comment.stream"
        }.count()
        .toString(),
    )
  }
}
