/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.room.internal

import jakarta.transaction.Transactional
import java.security.SecureRandom
import java.time.Instant
import java.util.Locale
import java.util.UUID
import kotlin.math.pow
import net.particify.arsnova.core4.room.Membership
import net.particify.arsnova.core4.room.Room
import net.particify.arsnova.core4.room.RoomRole
import net.particify.arsnova.core4.room.event.DemoRoomDuplicatedEvent
import net.particify.arsnova.core4.room.event.RoomCreatedEvent
import net.particify.arsnova.core4.room.event.RoomDeletedEvent
import net.particify.arsnova.core4.room.event.RoomDuplicatedEvent
import net.particify.arsnova.core4.room.exception.RoomNotFoundException
import net.particify.arsnova.core4.user.User
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service

@Service
class RoomServiceImpl(
    private val roomRepository: RoomRepository,
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val roomProperties: RoomProperties
) : RoomRepository by roomRepository {
  companion object {
    private val SHORT_ID_MAX: Int = (10.0.pow(Room.SHORT_ID_LENGTH) - 1).toInt()
  }

  private val secureRandom = SecureRandom()

  @Transactional
  fun create(room: Room, user: User): Room {
    val membership =
        Membership(
            room = room,
            user = user,
            role = RoomRole.OWNER,
            lastActivityAt = Instant.now(),
        )
    room.userRoles.add(membership)
    val persistedRoom = save(room)
    applicationEventPublisher.publishEvent(RoomCreatedEvent(persistedRoom.id!!))
    return persistedRoom
  }

  @Transactional
  override fun deleteById(id: UUID) {
    applicationEventPublisher.publishEvent(RoomDeletedEvent(id))
    roomRepository.deleteById(id)
  }

  @Transactional
  fun duplicate(room: Room, newName: String, user: User): Room {
    val newRoom = room.copy(generateShortId(), newName)
    val persistedRoom = create(newRoom, user)
    applicationEventPublisher.publishEvent(RoomDuplicatedEvent(room.id!!, persistedRoom.id!!))
    return persistedRoom
  }

  @Transactional
  fun duplicateDemo(user: User, locale: Locale): Room {
    val language = user.language ?: locale.language
    val demoProperties =
        roomProperties.demo.find { it.language == language }
            ?: roomProperties.demo.firstOrNull()
            ?: throw RoomNotFoundException()
    val demoRoom = findOneByShortId(demoProperties.shortId) ?: throw RoomNotFoundException()
    val persistedRoom = duplicate(demoRoom, demoRoom.name!!, user)
    applicationEventPublisher.publishEvent(DemoRoomDuplicatedEvent(persistedRoom.id!!))
    return persistedRoom
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
