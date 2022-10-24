package de.thm.arsnova.service.wsgateway.adapter

import com.auth0.jwt.exceptions.JWTVerificationException
import de.thm.arsnova.service.wsgateway.security.JwtTokenUtil
import de.thm.arsnova.service.wsgateway.service.RoomAccessService
import org.slf4j.LoggerFactory
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.messaging.support.MessageBuilder
import org.springframework.messaging.support.MessageHeaderAccessor
import org.springframework.stereotype.Component

@Component
class AuthChannelInterceptorAdapter(
  private val jwtTokenUtil: JwtTokenUtil,
  private val roomAccessService: RoomAccessService,
) : ChannelInterceptor {

  companion object {
    val topicStartString: String = "/topic/"
    val topicIndexBeforeRoomId: Int = topicStartString.length

    val topicRoomIdLength = 32
    val roomIdRegex: Regex = Regex("[0-9a-f]{$topicRoomIdLength}")

    val moderatorString = "moderator"

    val participantRoleString = "PARTICIPANT"
    val creatorRoleString = "CREATOR"
    val executiveModeratorRoleString = "EXECUTIVE_MODERATOR"
    val editingModeratorRoleString = "EDITING_MODERATOR"
  }

  private val logger = LoggerFactory.getLogger(AuthChannelInterceptorAdapter::class.java)

  override fun preSend(message: Message<*>, channel: MessageChannel): Message<*>? {
    logger.debug("Inspecting incoming message: {}", message)

    val accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor::class.java)!!
    val wsSessionId = accessor.sessionId!!

    /*
    accessor.command holds the STOMP command, accessor.destination the destination the client wants to send the message to.
    The destination could not be set (for example for ACK-Messages which are just a heartbeat).
     */

    if (accessor.command != null && accessor.command == StompCommand.CONNECT) {
      // user needs to authorize
      logger.trace("Incoming message is a connect command")
      val tokenList = accessor.getNativeHeader("token")
      if (tokenList != null && tokenList.size > 0) {
        val token = tokenList.first()!!
        try {
          val user = jwtTokenUtil.getUser(token)
          // Set the user so it is available for subsequent commands
          accessor.user = user
        } catch (e: JWTVerificationException) {
          return null
        }
      } else {
        // no token given -> auth failed
        logger.debug("no auth token given, dropping connection attempt")
        return null
      }
    } else {
      logger.trace("Incoming message is anything but a connect command")
      val userId = accessor.user?.name
      if (userId == null) {
        logger.debug("User didn't authenticate himself, dropping message. WebSocket session id: {}", wsSessionId)
        return null
      }

      if (accessor.destination != null && accessor.command != null && accessor.command == StompCommand.SUBSCRIBE) {
        val destination: String = accessor.destination!!
        val roomId = destination.substring(topicIndexBeforeRoomId, topicIndexBeforeRoomId + topicRoomIdLength)
        if (!roomId.matches(roomIdRegex)) {
          logger.debug("Didn't get a valid roomId out of the destination: {}", destination)
          return null
        }
        logger.trace("Extracted roomId from subscribe message: {}", roomId)

        logger.debug("Incoming message is a subscribe command")
        if (moderatorString in destination) {
          logger.trace("Noticed a moderator role in topic, checking for auth")
          val userRoomAccess = roomAccessService.getRoomAccess(roomId, userId)
          val allowedRoles = listOf(creatorRoleString, executiveModeratorRoleString, editingModeratorRoleString)
          if (!allowedRoles.contains(userRoomAccess.role)) {
            logger.debug("User doesn't have any auth information, dropping the message")
            return null
          }
          logger.debug("Got room access info: {}", userRoomAccess)
        }
      }
    }

    // default is to pass the frame along
    return MessageBuilder.createMessage(message.payload, accessor.messageHeaders)
  }
}
