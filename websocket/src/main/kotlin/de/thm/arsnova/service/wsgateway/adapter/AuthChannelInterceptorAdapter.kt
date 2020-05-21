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
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
class AuthChannelInterceptorAdapter(
		private val jwtTokenUtil: JwtTokenUtil,
		private val roomAccessService: RoomAccessService
) : ChannelInterceptor {

	companion object {
		val topicStartString: String = "/topic/"
		val topicIndexBeforeRoomId: Int = topicStartString.length

		val topicRoomIdLength = 32
		val roomIdRegex: Regex = Regex("[0-9a-f]{$topicRoomIdLength}")

		val moderatorString = "moderator"
	}

	private val logger = LoggerFactory.getLogger(AuthChannelInterceptorAdapter::class.java)
	/* for the new STOMP over ws functionality */
	private val wsSessionIdToUserId = ConcurrentHashMap<String, String>()

	override fun preSend(message: Message<*>, channel: MessageChannel): Message<*>? {
		logger.info("Inspecting incoming message: {}", message)

		val accessor = StompHeaderAccessor.wrap(message)
		val wsSessionId = accessor.sessionId!!

		if (accessor.command != null && accessor.command == StompCommand.CONNECT) {
			// user needs to authorize
			logger.trace("Incoming message is a connect command")
			val tokenList = accessor.getNativeHeader("token")
			if (tokenList != null && tokenList.size > 0) {
				val token = tokenList.first()!!
				try {
					val userId = jwtTokenUtil.getUserId(token)
					logger.trace("Adding token {} to the ws session mapping", tokenList[0])
					wsSessionIdToUserId.put(wsSessionId, userId)
				} catch (e: JWTVerificationException) {
					return null
				}
			} else {
				// no token given -> auth failed
				logger.info("no auth token given, dropping connection attempt")
				return null;
			}
		} else {
			logger.trace("Incoming message is anything but a connect command")
			val userId = wsSessionIdToUserId.getOrDefault(wsSessionId, null)
			if (userId == null) {
				logger.debug("User didn't authenticate himself, dropping message. WebSocket session id: {}", wsSessionId);
				return null
			}

			if (accessor.command != null && accessor.command == StompCommand.SUBSCRIBE) {
				logger.info("Incoming message is a subscribe command")
				val destination: String = accessor.destination!!
				val roomId = destination.substring(topicIndexBeforeRoomId, topicIndexBeforeRoomId + topicRoomIdLength)
				if (!roomId.matches(roomIdRegex)) {
					logger.debug("Didn't get a valid roomId out of the destination: {}", destination)
					return null
				}
				logger.trace("Extracted roomId from subscribe message: {}", roomId)
				if (moderatorString in destination) {
					logger.trace("Noticed a moderator role in topic, checking for auth")
					// For now, if there is auth information stored, it's either moderator or owner. No need to check specifically
					val userRoomAccess = roomAccessService.getRoomAccess(roomId, userId)
					if (userRoomAccess == null) {
						logger.debug("User doesn't have any auth information, dropping the message")
						return null
					}
					logger.error("Got room access info: {}", userRoomAccess)
				}
			}
		}

		// default is to pass the frame along
		return MessageBuilder.createMessage(message.payload, accessor.messageHeaders)
	}
}
