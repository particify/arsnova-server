/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.room.internal

import java.security.SecureRandom
import java.time.Instant
import java.util.UUID
import kotlin.math.pow
import net.particify.arsnova.core4.room.Membership
import net.particify.arsnova.core4.room.Room
import net.particify.arsnova.core4.room.RoomRole
import net.particify.arsnova.core4.user.User
import org.springframework.stereotype.Service

@Service
class RoomServiceImpl(private val roomRepository: RoomRepository) :
    RoomRepository by roomRepository {
  companion object {
    private val SHORT_ID_MAX: Int = (10.0.pow(Room.SHORT_ID_LENGTH) - 1).toInt()
  }

  private val secureRandom = SecureRandom()

  fun create(room: Room, user: User): Room {
    val membership =
        Membership(
            room = room,
            user = user,
            role = RoomRole.OWNER,
            lastActivityAt = Instant.now(),
        )
    room.userRoles.add(membership)
    return save(room)
  }

  override fun deleteById(id: UUID) {
    roomRepository.deleteById(id)
  }

  fun generateShortId(): Int {
    val shortId = secureRandom.nextInt(0, (SHORT_ID_MAX))
    if (countByShortId(shortId) == 0) {
      return shortId
    }
    return generateShortId()
  }

  @Deprecated(
      "Deprecated by base implementation", replaceWith = ReplaceWith("deleteAllInBatch(entities)"))
  override fun deleteInBatch(entities: Iterable<Room>) = roomRepository.deleteInBatch(entities)
}
