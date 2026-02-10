/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.room.internal.api

import java.time.Duration
import java.time.Instant
import java.util.Locale
import java.util.UUID
import net.particify.arsnova.core4.common.LanguageIso639
import net.particify.arsnova.core4.common.exception.InvalidInputException
import net.particify.arsnova.core4.room.Membership
import net.particify.arsnova.core4.room.Room
import net.particify.arsnova.core4.room.RoomRole
import net.particify.arsnova.core4.room.exception.MembershipNotFoundException
import net.particify.arsnova.core4.room.exception.RoomNotFoundException
import net.particify.arsnova.core4.room.internal.MembershipServiceImpl
import net.particify.arsnova.core4.room.internal.RoomServiceImpl
import net.particify.arsnova.core4.user.LocalUserService
import net.particify.arsnova.core4.user.User
import org.springframework.data.repository.findByIdOrNull
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller

private const val LAST_ACTIVITY_MINIMUM_DIFFERENCE_SECONDS = 5L

@Controller
@PreAuthorize("hasRole('USER')")
@SchemaMapping(typeName = "Mutation")
class RoomMutationController(
    private val roomService: RoomServiceImpl,
    private val membershipService: MembershipServiceImpl,
    private val localUserService: LocalUserService
) {

  @MutationMapping
  fun createRoom(@Argument input: CreateRoomInput, @AuthenticationPrincipal user: User): Room {
    return roomService.create(input.toRoom(roomService.generateShortId()), user)
  }

  @MutationMapping
  fun joinRoom(@Argument input: JoinRoomInput, @AuthenticationPrincipal user: User): Membership {
    if (input.id == null && input.shortId == null) {
      throw InvalidInputException("One of id or shortId is required.")
    }
    val room =
        (if (input.id != null) roomService.findByIdOrNull(input.id)
        else roomService.findOneByShortId(input.shortId!!.toInt()))
            ?: throw RoomNotFoundException(input.id)
    var membership = membershipService.findOneByRoomIdAndUserId(room.id!!, user.id!!)
    if (membership == null) {
      membership =
          Membership(
              room = room, user = user, role = RoomRole.PARTICIPANT, lastActivityAt = Instant.now())
      membershipService.save(membership)
    } else if (membership.lastActivityAt == null ||
        membership.lastActivityAt!! <
            Instant.now().minus(Duration.ofSeconds(LAST_ACTIVITY_MINIMUM_DIFFERENCE_SECONDS))) {
      membership.lastActivityAt = Instant.now()
      membershipService.save(membership)
    }
    return membership
  }

  @MutationMapping
  @PreAuthorize("hasPermission(#id, 'Room', 'write')")
  fun updateRoomName(@Argument id: UUID, @Argument name: String): Room {
    val room = roomService.findByIdOrNull(id) ?: throw RoomNotFoundException(id)
    room.name = name
    return roomService.save(room)
  }

  @MutationMapping
  @PreAuthorize("hasPermission(#id, 'Room', 'write')")
  fun updateRoomDescription(@Argument id: UUID, @Argument description: String): Room {
    val room = roomService.findByIdOrNull(id) ?: throw RoomNotFoundException(id)
    room.description = description
    return roomService.save(room)
  }

  @MutationMapping
  @PreAuthorize("hasPermission(#id, 'Room', 'write')")
  fun updateRoomLanguage(
      @Argument id: UUID,
      @Argument @LanguageIso639 languageCode: String?
  ): Room {
    val room = roomService.findByIdOrNull(id) ?: throw RoomNotFoundException(id)
    room.language = languageCode
    return roomService.save(room)
  }

  @MutationMapping
  @PreAuthorize("hasPermission(#id, 'Room', 'write')")
  fun updateRoomFocusMode(@Argument id: UUID, @Argument enabled: Boolean): Room {
    val room = roomService.findByIdOrNull(id) ?: throw RoomNotFoundException(id)
    room.focusModeEnabled = enabled
    return roomService.save(room)
  }

  @MutationMapping
  @PreAuthorize("hasPermission(#id, 'Room', 'delete')")
  fun deleteRoom(@Argument id: UUID): UUID {
    roomService.deleteById(id)
    return id
  }

  @MutationMapping
  @PreAuthorize("hasPermission(#roomId, 'Room', 'administer')")
  fun grantRoomRole(
      @Argument roomId: UUID,
      @Argument userId: UUID,
      @Argument role: RoomRole
  ): Membership {
    if (role == RoomRole.OWNER || role == RoomRole.PARTICIPANT) {
      throw InvalidInputException("Changing role to owner or participant is not allowed.")
    }
    var membership = membershipService.findByIdOrNull(Membership.RoomUserId(roomId, userId))
    if (membership == null) {
      membership = Membership(room = Room(id = roomId), user = User(id = userId))
    }
    if (membership.role == RoomRole.OWNER) {
      throw InvalidInputException("Changing of current owner's role is not allowed.")
    }
    membership.role = role
    return membershipService.save(membership)
  }

  @MutationMapping
  @PreAuthorize("hasPermission(#roomId, 'Room', 'administer')")
  fun grantRoomRoleByInvitation(
      @Argument roomId: UUID,
      @Argument mailAddress: String,
      @Argument role: RoomRole,
      @AuthenticationPrincipal user: User,
      locale: Locale
  ): Membership {
    if (role == RoomRole.OWNER || role == RoomRole.PARTICIPANT) {
      throw InvalidInputException("Changing role to owner or participant is not allowed.")
    }
    val room = roomService.findByIdOrNull(roomId) ?: error("Room not found.")
    val invitee =
        localUserService.inviteUser(
            user, mailAddress, "invitation-verification", mapOf("room" to room), locale)
    val membership = Membership(room = Room(id = roomId), user = User(id = invitee.id))
    membership.role = role
    return membershipService.save(membership)
  }

  @MutationMapping
  @PreAuthorize("hasPermission(#roomId, 'Room', 'administer')")
  fun revokeRoomRole(@Argument roomId: UUID, @Argument userId: UUID): Membership {
    val id = Membership.RoomUserId(roomId, userId)
    val membership = membershipService.findByIdOrNull(id) ?: throw MembershipNotFoundException(id)
    if (membership.role == RoomRole.OWNER) {
      throw InvalidInputException("Revoking of current owner's role is not allowed.")
    }
    membership.role = RoomRole.PARTICIPANT
    return membershipService.save(membership)
  }

  @MutationMapping
  @PreAuthorize("hasPermission(#roomId, 'Room', 'read')")
  fun revokeRoomMembership(
      @Argument roomId: UUID,
      @AuthenticationPrincipal user: User
  ): Membership {
    val id = Membership.RoomUserId(roomId, user.id)
    val membership = membershipService.findByIdOrNull(id) ?: throw MembershipNotFoundException(id)
    if (membership.role == RoomRole.OWNER) {
      throw InvalidInputException("Revoking of current owner's membership is not allowed.")
    }
    membershipService.delete(membership)
    return membership
  }

  @MutationMapping
  @PreAuthorize("hasPermission(#input.id, 'Room', 'administer')")
  fun duplicateRoom(
      @Argument input: DuplicateRoomInput,
      @AuthenticationPrincipal user: User
  ): Room {
    val room = roomService.findByIdOrNull(input.id) ?: throw RoomNotFoundException(input.id)
    return roomService.duplicate(room, input.newName, user)
  }

  @MutationMapping
  fun duplicateDemoRoom(@AuthenticationPrincipal user: User, locale: Locale): Room {
    return roomService.duplicateDemo(user, locale)
  }
}
