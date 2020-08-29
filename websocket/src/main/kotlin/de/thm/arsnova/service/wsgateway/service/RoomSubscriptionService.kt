package de.thm.arsnova.service.wsgateway.service

import kotlinx.coroutines.GlobalScope
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.launch

object RoomSubscriptionService {
	// roomId -> Set<userIds>
	private val roomUsers = ConcurrentHashMap<String, MutableSet<String>>()
	// userId -> roomId
	// used for reverse lookup when user leaves room
	private val userInRoom = ConcurrentHashMap<String, String>()

	fun addUser(roomId: String, userId: String) = GlobalScope.launch {
		synchronized(roomUsers) {
			val currentUsers: MutableSet<String> = roomUsers.getOrDefault(roomId, mutableSetOf())
			currentUsers.add(userId)
			roomUsers[roomId] = currentUsers
			userInRoom[userId] = roomId
		}
	}

	fun removeUser(userId: String) = GlobalScope.launch {
		synchronized(roomUsers) {
			val roomId = userInRoom[userId]
			if (roomId != null) {
				val currentUsers: MutableSet<String> = roomUsers.getOrDefault(roomId, mutableSetOf())
				currentUsers.removeIf { it == userId }
				roomUsers[roomId] = currentUsers
				userInRoom.remove(userId)
			}
		}
	}

	fun getUserCount(roomId: String): Int? {
		return roomUsers.get(roomId)?.count()
	}
}
