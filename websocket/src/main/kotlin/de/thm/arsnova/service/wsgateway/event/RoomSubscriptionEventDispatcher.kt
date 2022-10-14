package de.thm.arsnova.service.wsgateway.event

import de.thm.arsnova.service.wsgateway.model.RoomSubscription
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.stereotype.Component
import org.springframework.web.socket.messaging.SessionDisconnectEvent
import org.springframework.web.socket.messaging.SessionSubscribeEvent
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent
import java.util.concurrent.ConcurrentHashMap

@Component
class RoomSubscriptionEventDispatcher(
  private val applicationEventPublisher: ApplicationEventPublisher,
) {
  private val logger = LoggerFactory.getLogger(this::class.java)
  private val roomTopicPattern = Regex("^/topic/([0-9a-f]{32})\\.stream$")
  private val wsSessionIdToSubscriptionMapping = ConcurrentHashMap<String, RoomSubscription>()

  fun getWsSessionCount(): Int {
    return wsSessionIdToSubscriptionMapping.size
  }

  @EventListener
  fun dispatchSubscribeEvent(event: SessionSubscribeEvent) {
    val accessor = StompHeaderAccessor.wrap(event.message)
    logger.trace("Handling session subscribe event: {}", accessor)
    val result = roomTopicPattern.find(accessor.destination!!) ?: return
    val roomId = result.groups[1]!!.value
    val userId = accessor.user?.name ?: return
    val roomSubscription: RoomSubscription
    synchronized(wsSessionIdToSubscriptionMapping) {
      val oldRoomSubscription = wsSessionIdToSubscriptionMapping[accessor.sessionId]
      if (oldRoomSubscription != null) {
        applicationEventPublisher.publishEvent(
          RoomLeaveEvent(
            wsSessionId = accessor.sessionId!!,
            userId = userId,
            roomId = oldRoomSubscription.roomId,
          )
        )
      }
      roomSubscription = RoomSubscription(
        subscriptionId = accessor.subscriptionId!!,
        roomId = roomId,
      )
      logger.debug("Adding WS session -> subscription mapping: {} -> {}, ", accessor.sessionId, roomSubscription)
      wsSessionIdToSubscriptionMapping[accessor.sessionId!!] = roomSubscription
      applicationEventPublisher.publishEvent(
        RoomJoinEvent(
          wsSessionId = accessor.sessionId!!,
          userId = userId,
          roomId = roomSubscription.roomId,
        )
      )
    }
  }

  @EventListener
  fun dispatchUnsubscribeEvent(event: SessionUnsubscribeEvent) {
    val accessor = StompHeaderAccessor.wrap(event.message)
    logger.trace("Handling session unsubscribe event: {}", accessor)
    val userId = accessor.user?.name ?: return
    synchronized(wsSessionIdToSubscriptionMapping) {
      val roomSubscription = wsSessionIdToSubscriptionMapping[accessor.sessionId]
      if (roomSubscription == null || accessor.subscriptionId != roomSubscription.subscriptionId) {
        return
      }
      logger.debug("Removing WS session -> subscription mapping: {} -> {}, ", accessor.sessionId, roomSubscription)
      wsSessionIdToSubscriptionMapping.remove(accessor.sessionId)
      applicationEventPublisher.publishEvent(
        RoomLeaveEvent(
          wsSessionId = accessor.sessionId!!,
          userId = userId,
          roomId = roomSubscription.roomId,
        )
      )
    }
  }

  @EventListener
  fun dispatchDisconnectEvent(event: SessionDisconnectEvent) {
    val accessor = StompHeaderAccessor.wrap(event.message)
    logger.trace("Handling session disconnect event: {}", accessor)
    val userId = accessor.user?.name ?: return
    synchronized(wsSessionIdToSubscriptionMapping) {
      val roomSubscription = wsSessionIdToSubscriptionMapping[accessor.sessionId] ?: return
      logger.debug("Removing WS session -> subscription mapping: {} -> {}, ", accessor.sessionId, roomSubscription)
      wsSessionIdToSubscriptionMapping.remove(accessor.sessionId)
      applicationEventPublisher.publishEvent(
        RoomLeaveEvent(
          wsSessionId = accessor.sessionId!!,
          userId = userId,
          roomId = roomSubscription.roomId,
        )
      )
    }
  }
}
