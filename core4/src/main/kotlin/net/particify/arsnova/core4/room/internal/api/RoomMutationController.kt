/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.room.internal.api

import java.security.SecureRandom
import java.time.Instant
import java.util.Locale
import java.util.UUID
import kotlin.math.pow
import net.particify.arsnova.core4.common.LanguageIso639
import net.particify.arsnova.core4.room.Membership
import net.particify.arsnova.core4.room.Room
import net.particify.arsnova.core4.room.RoomRole
import net.particify.arsnova.core4.room.internal.MembershipRepository
import net.particify.arsnova.core4.room.internal.RoomRepository
import net.particify.arsnova.core4.user.LocalUserService
import net.particify.arsnova.core4.user.User
import org.springframework.data.repository.findByIdOrNull
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller

@Controller
@SchemaMapping(typeName = "Mutation")
class RoomMutationController(
    private val roomRepository: RoomRepository,
    private val membershipRepository: MembershipRepository,
    private val localUserService: LocalUserService
) {
  companion object {
    private val SHORT_ID_MAX: Int = (10.0.pow(Room.SHORT_ID_LENGTH) - 1).toInt()
  }

  private val secureRandom = SecureRandom()

  @MutationMapping
  fun createRoom(@Argument input: CreateRoomInput, @AuthenticationPrincipal user: User): Room {
    val room = input.toRoom(shortId = generateShortId())
    val membership =
        Membership(
            room = room,
            user = user,
            role = RoomRole.OWNER,
            lastActivityAt = Instant.now(),
        )
    room.userRoles.add(membership)
    val persistedRoom = roomRepository.save(room)
    return persistedRoom
  }

  private fun generateShortId(): Int {
    val shortId = secureRandom.nextInt(0, (SHORT_ID_MAX))
    if (roomRepository.countByShortId(shortId) == 0) {
      return shortId
    }
    return generateShortId()
  }

  @MutationMapping
  fun joinRoom(@Argument input: JoinRoomInput, @AuthenticationPrincipal user: User): Membership {
    assert(input.id != null || input.shortId != null) { "One of id or shortId is required." }
    val room =
        if (input.id != null) roomRepository.findOneById(input.id)
        else roomRepository.findOneByShortId(input.shortId!!.toInt())
    var membership = membershipRepository.findOneByRoomIdAndUserId(room.id!!, user.id!!)
    if (membership == null) {
      membership = Membership(room = room, user = user, role = RoomRole.PARTICIPANT)
      membershipRepository.save(membership)
    }
    return membership
  }

  @MutationMapping
  fun updateRoomName(@Argument id: UUID, @Argument name: String): Room {
    val room = roomRepository.findOneById(id)
    room.name = name
    return roomRepository.save(room)
  }

  @MutationMapping
  fun updateRoomDescription(@Argument id: UUID, @Argument description: String): Room {
    val room = roomRepository.findOneById(id)
    room.description = description
    return roomRepository.save(room)
  }

  @MutationMapping
  fun updateRoomLanguage(
      @Argument id: UUID,
      @Argument @LanguageIso639 languageCode: String?
  ): Room {
    val room = roomRepository.findOneById(id)
    room.language = languageCode
    return roomRepository.save(room)
  }

  @MutationMapping
  fun updateRoomFocusMode(@Argument id: UUID, @Argument enabled: Boolean): Room {
    val room = roomRepository.findOneById(id)
    room.focusModeEnabled = enabled
    return roomRepository.save(room)
  }

  @MutationMapping
  fun deleteRoom(@Argument id: UUID): Boolean {
    roomRepository.deleteById(id)
    return true
  }

  @MutationMapping
  fun grantRoomRole(
      @Argument roomId: UUID,
      @Argument userId: UUID,
      @Argument role: RoomRole
  ): Boolean {
    if (role == RoomRole.OWNER || role == RoomRole.PARTICIPANT) {
      error("Changing role to owner or participant is not allowed.")
    }
    var membership = membershipRepository.findByIdOrNull(Membership.RoomUserId(roomId, userId))
    if (membership == null) {
      membership = Membership(room = Room(id = roomId), user = User(id = userId))
    }
    if (membership.role == RoomRole.OWNER) {
      error("Change role of owner is not allowed.")
    }
    membership.role = role
    membershipRepository.save(membership)
    return true
  }

  @MutationMapping
  fun grantRoomRoleByInvitation(
      @Argument roomId: UUID,
      @Argument mailAddress: String,
      @Argument role: RoomRole,
      @AuthenticationPrincipal user: User,
      locale: Locale
  ): Boolean {
    if (role == RoomRole.OWNER || role == RoomRole.PARTICIPANT) {
      error("Changing role to owner or participant is not allowed.")
    }
    val room = roomRepository.findByIdOrNull(roomId) ?: error("Room not found.")
    val invitee =
        localUserService.inviteUser(
            user, mailAddress, "invitation-verification", mapOf("room" to room), locale)
    val membership = Membership(room = Room(id = roomId), user = User(id = invitee.id))
    membership.role = role
    membershipRepository.save(membership)
    return true
  }

  @MutationMapping
  fun revokeRoomRole(@Argument roomId: UUID, @Argument userId: UUID): Boolean {
    val membership =
        membershipRepository.findByIdOrNull(Membership.RoomUserId(roomId, userId))
            ?: error("No membership found.")
    if (membership.role == RoomRole.OWNER) {
      error("Revoking role of owner is not allowed.")
    }
    membership.role = RoomRole.PARTICIPANT
    membershipRepository.save(membership)
    return true
  }
}
