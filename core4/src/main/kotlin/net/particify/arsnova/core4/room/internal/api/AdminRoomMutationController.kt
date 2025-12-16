/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.room.internal.api

import java.util.UUID
import net.particify.arsnova.core4.room.Membership
import net.particify.arsnova.core4.room.Room
import net.particify.arsnova.core4.room.RoomRole
import net.particify.arsnova.core4.room.exception.MembershipNotFoundException
import net.particify.arsnova.core4.room.internal.MembershipServiceImpl
import net.particify.arsnova.core4.room.internal.RoomServiceImpl
import net.particify.arsnova.core4.user.UserService
import net.particify.arsnova.core4.user.exception.UserNotFoundException
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

@Controller
@PreAuthorize("hasRole('ADMIN')")
@SchemaMapping(typeName = "Mutation")
class AdminRoomMutationController(
    private val roomService: RoomServiceImpl,
    private val userService: UserService,
    private val membershipService: MembershipServiceImpl
) {
  @MutationMapping
  fun adminDeleteRoomById(@Argument id: UUID): UUID {
    roomService.deleteById(id)
    return id
  }

  @MutationMapping
  fun adminTransferRoom(@Argument roomId: UUID, @Argument userId: UUID): Room? {
    val user = userService.loadUserById(userId) ?: throw UserNotFoundException(userId)
    val oldMembership =
        membershipService.findOwnerMembershipByRoomId(roomId) ?: throw MembershipNotFoundException()
    oldMembership.role = RoomRole.PARTICIPANT
    this.membershipService.save(oldMembership)
    val membership =
        membershipService.findOneByRoomIdAndUserId(roomId, userId)
            ?: Membership(room = Room(id = roomId), user = user)
    membership.role = RoomRole.OWNER
    return membershipService.save(membership).room
  }
}
