package de.thm.arsnova.service.wsgateway.service

import de.thm.arsnova.service.wsgateway.event.UserCountChanged
import kotlinx.coroutines.GlobalScope
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Service

@Service
class RoomSubscriptionService(
		private val rabbitTemplate: RabbitTemplate
) {
	private val logger = LoggerFactory.getLogger(RoomSubscriptionService::class.java)

	// roomId -> Set<userIds>
	private val roomUsers = ConcurrentHashMap<String, MutableSet<String>>()
	// userId -> roomId
	// used for reverse lookup when user leaves room
	private val userInRoom = ConcurrentHashMap<String, String>()

	fun addUser(roomId: String, userId: String) = GlobalScope.launch {
		var currentUsers: MutableSet<String>
		synchronized(roomUsers) {
			currentUsers = roomUsers.getOrDefault(roomId, mutableSetOf())
			currentUsers.add(userId)
			roomUsers[roomId] = currentUsers
			userInRoom[userId] = roomId
		}
		rabbitTemplate.convertAndSend(
				"amq.topic",
				"${roomId}.stream",
				UserCountChanged(currentUsers.count())
		)
	}

	fun removeUser(userId: String) = GlobalScope.launch {
		var roomId: String? = null
		var currentUsers: MutableSet<String> = mutableSetOf()
		synchronized(roomUsers) {
			roomId = userInRoom[userId]
			if (roomId != null) {
				currentUsers = roomUsers.getOrDefault(roomId!!, mutableSetOf())
				currentUsers.removeIf { it == userId }
				roomUsers[roomId!!] = currentUsers
				userInRoom.remove(userId)
			}
		}
		if (roomId != null) {
			rabbitTemplate.convertAndSend(
					"amq.topic",
					"${roomId}.stream",
					UserCountChanged(currentUsers.count())
			)
		}
	}

	fun getUserCount(roomId: String): Int? {
		return roomUsers.get(roomId)?.count()
	}

	fun getUserCount(): Int {
		return userInRoom.size
	}
}
