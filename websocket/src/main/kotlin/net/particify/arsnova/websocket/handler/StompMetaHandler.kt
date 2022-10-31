package net.particify.arsnova.websocket.handler

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.messaging.simp.user.SimpUserRegistry
import org.springframework.stereotype.Component

@Component
class StompMetaHandler {
  private val logger = LoggerFactory.getLogger(StompMetaHandler::class.java)
  private lateinit var simpUserRegistry: SimpUserRegistry

  @Autowired
  fun setSimpUserRegistry(simpUserRegistry: SimpUserRegistry) {
    this.simpUserRegistry = simpUserRegistry
  }

  fun handleUserSubscribed(roomId: String) {
    logger.error("/topic/$roomId.comment.stream")
    logger.error(
      simpUserRegistry.findSubscriptions {
        it.destination == "/topic/$roomId.comment.stream"
      }.count().toString()
    )
  }
}
