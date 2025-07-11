package net.particify.arsnova.websocket.service

import io.github.bucket4j.BandwidthBuilder
import io.github.bucket4j.Bucket
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.particify.arsnova.websocket.config.WebSocketProperties
import net.particify.arsnova.websocket.event.RoomJoinEvent
import net.particify.arsnova.websocket.event.RoomLeaveEvent
import net.particify.arsnova.websocket.event.RoomUserCountChangedEvent
import net.particify.arsnova.websocket.event.UserCountChanged
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

@Service
class RoomSubscriptionService(
  private val rabbitTemplate: RabbitTemplate,
  private val webSocketProperties: WebSocketProperties,
  private val applicationEventPublisher: ApplicationEventPublisher,
) {
  private val logger = LoggerFactory.getLogger(RoomSubscriptionService::class.java)

  // roomId -> Set<userIds>
  private val roomUsers = ConcurrentHashMap<String, MutableSet<String>>()

  private val threshold = webSocketProperties.gateway.eventRateLimit.threshold
  private val duration = webSocketProperties.gateway.eventRateLimit.duration
  private val tokensPerTimeframe = webSocketProperties.gateway.eventRateLimit.tokensPerTimeframe
  private val burstCapacity = webSocketProperties.gateway.eventRateLimit.burstCapacity

  private val eventBucketMap: MutableMap<String, Bucket> = ConcurrentHashMap()

  fun addUser(
    roomId: String,
    userId: String,
  ) = GlobalScope.launch {
    var currentUsers: MutableSet<String>
    synchronized(roomUsers) {
      currentUsers = roomUsers.getOrDefault(roomId, mutableSetOf())
      currentUsers.add(userId)
      roomUsers[roomId] = currentUsers
    }
    sendUserCountChangedEvent(roomId, currentUsers.count())
    applicationEventPublisher.publishEvent(RoomUserCountChangedEvent(roomId, currentUsers.count()))
  }

  fun removeUser(
    roomId: String,
    userId: String,
  ) = GlobalScope.launch {
    var currentUsers: MutableSet<String>
    synchronized(roomUsers) {
      currentUsers = roomUsers.getOrDefault(roomId, mutableSetOf())
      currentUsers.remove(userId)
      if (currentUsers.isEmpty()) {
        roomUsers.remove(roomId)
      } else {
        roomUsers[roomId] = currentUsers
      }
    }
    sendUserCountChangedEvent(roomId, currentUsers.count())
    applicationEventPublisher.publishEvent(RoomUserCountChangedEvent(roomId, currentUsers.count()))
  }

  fun getUserCount(roomId: String): Int? = roomUsers.get(roomId)?.count()

  fun getUserCounts(): List<Int> = this.roomUsers.map { (roomId, userIds) -> userIds.size }

  fun getUserCountsMap(): Map<String, Int> = this.roomUsers.mapValues { (roomId, userIds) -> userIds.size }

  fun sendUserCountChangedEvent(
    roomId: String,
    currentUserCount: Int,
  ) {
    var canSend = true
    if (currentUserCount > threshold) {
      val bucket: Bucket =
        eventBucketMap.computeIfAbsent(roomId) {
          val bandwidth =
            BandwidthBuilder
              .builder()
              .capacity(burstCapacity)
              .refillIntervally(tokensPerTimeframe, duration)
              .build()
          Bucket.builder().addLimit(bandwidth).build()
        }
      val probe = bucket.tryConsumeAndReturnRemaining(1)
      if (!probe.isConsumed) {
        canSend = false
      }
    }
    if (canSend) {
      rabbitTemplate.convertAndSend(
        "amq.topic",
        "$roomId.stream",
        UserCountChanged(currentUserCount),
      )
    }
  }

  @EventListener
  fun handleRoomJoinEvent(event: RoomJoinEvent) {
    addUser(event.roomId, event.userId)
  }

  @EventListener
  fun handleRoomLeaveEvent(event: RoomLeaveEvent) {
    removeUser(event.roomId, event.userId)
  }
}
