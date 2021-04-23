package de.thm.arsnova.service.wsgateway.service

import de.thm.arsnova.service.wsgateway.config.WebSocketProperties
import de.thm.arsnova.service.wsgateway.event.RoomJoinEvent
import de.thm.arsnova.service.wsgateway.event.RoomLeaveEvent
import de.thm.arsnova.service.wsgateway.event.UserCountChanged
import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import io.github.bucket4j.Bucket4j
import io.github.bucket4j.Refill
import kotlinx.coroutines.GlobalScope
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service

@Service
class RoomSubscriptionService(
		private val rabbitTemplate: RabbitTemplate,
		private val webSocketProperties: WebSocketProperties
) {
	private val logger = LoggerFactory.getLogger(RoomSubscriptionService::class.java)

	// roomId -> Set<userIds>
	private val roomUsers = ConcurrentHashMap<String, MutableSet<String>>()

	private val threshold = webSocketProperties.gateway.eventRateLimit.threshold
	private val duration = webSocketProperties.gateway.eventRateLimit.duration
	private val tokensPerTimeframe = webSocketProperties.gateway.eventRateLimit.tokensPerTimeframe
	private val burstCapacity = webSocketProperties.gateway.eventRateLimit.burstCapacity

	private val eventBucketMap: MutableMap<String, Bucket> = ConcurrentHashMap()

	fun addUser(roomId: String, userId: String) = GlobalScope.launch {
		var currentUsers: MutableSet<String>
		synchronized(roomUsers) {
			currentUsers = roomUsers.getOrDefault(roomId, mutableSetOf())
			currentUsers.add(userId)
			roomUsers[roomId] = currentUsers
		}
		sendUserCountChangedEvent(roomId, currentUsers.count())
	}

	fun removeUser(roomId: String, userId: String) = GlobalScope.launch {
		var currentUsers: MutableSet<String>
		synchronized(roomUsers) {
			currentUsers = roomUsers.getOrDefault(roomId, mutableSetOf())
			currentUsers.remove(userId)
			roomUsers[roomId] = currentUsers
		}
		sendUserCountChangedEvent(roomId, currentUsers.count())
	}

	fun getUserCount(roomId: String): Int? {
		return roomUsers.get(roomId)?.count()
	}

	fun sendUserCountChangedEvent(roomId: String, currentUserCount: Int) {
		var canSend = true
		if (currentUserCount > threshold) {
			val bucket: Bucket = eventBucketMap.computeIfAbsent(roomId) {
				val refill: Refill = Refill.intervally(tokensPerTimeframe, duration)
				val limit: Bandwidth = Bandwidth.classic(burstCapacity, refill)
				Bucket4j.builder().addLimit(limit).build()
			}
			val probe = bucket.tryConsumeAndReturnRemaining(1)
			if (!probe.isConsumed) {
				canSend = false
			}
		}
		if (canSend) {
			rabbitTemplate.convertAndSend(
				"amq.topic",
				"${roomId}.stream",
				UserCountChanged(currentUserCount)
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
