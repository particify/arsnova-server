/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.room

import java.util.UUID
import net.particify.arsnova.core4.room.internal.MembershipServiceImpl
import net.particify.arsnova.core4.room.internal.RoomBulkDeletionService
import net.particify.arsnova.core4.room.internal.RoomServiceImpl
import net.particify.arsnova.core4.room.internal.RoomUserAliasServiceImpl
import org.junit.jupiter.api.Test
import org.mockito.Mockito

class RoomBulkDeletionServiceTest {
  private val roomService = Mockito.mock(RoomServiceImpl::class.java)
  private val membershipService = Mockito.mock(MembershipServiceImpl::class.java)
  private val roomUserAliasService = Mockito.mock(RoomUserAliasServiceImpl::class.java)
  private val service =
      RoomBulkDeletionService(roomService, membershipService, roomUserAliasService)

  @Test
  fun shouldDeleteTheUsersAliasesOwnedRoomsAndMemberships() {
    val userId = UUID.randomUUID()
    val ownedRoomId = UUID.randomUUID()
    Mockito.`when`(membershipService.findIdsByIdUserIdAndIsOwner(userId))
        .thenReturn(listOf(Membership.RoomUserId(ownedRoomId, userId)))

    service.deleteRoomsAndMembershipsByUserId(userId)

    val inOrder = Mockito.inOrder(roomUserAliasService, roomService, membershipService)
    inOrder.verify(roomUserAliasService).deleteAllByUserId(userId)
    inOrder.verify(roomService).deleteById(ownedRoomId)
    inOrder.verify(membershipService).deleteAllByIdUserId(userId)
  }
}
