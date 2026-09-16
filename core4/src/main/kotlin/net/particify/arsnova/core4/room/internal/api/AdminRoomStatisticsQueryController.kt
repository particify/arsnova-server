/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.room.internal.api

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import java.time.Instant
import net.particify.arsnova.core4.room.AdminActiveRoomStats
import net.particify.arsnova.core4.room.AdminRoomActivityStats
import net.particify.arsnova.core4.room.AdminRoomStats
import net.particify.arsnova.core4.room.internal.MembershipServiceImpl
import net.particify.arsnova.core4.room.internal.RoomServiceImpl
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

private const val ACTIVITY_WINDOW_MINUTES_LOWER_BOUND = 1L
private const val ACTIVITY_WINDOW_MINUTES_UPPER_BOUND = 1440L
private const val MIN_MEMBER_COUNT_LOWER_BOUND = 1L

@Controller
@PreAuthorize("hasRole('ADMIN')")
@SchemaMapping(typeName = "Query")
class AdminRoomStatisticsQueryController(
    private val roomService: RoomServiceImpl,
    private val membershipServiceImpl: MembershipServiceImpl
) {

  @QueryMapping
  fun adminActiveRoomStats(
      @Argument @Min(MIN_MEMBER_COUNT_LOWER_BOUND) minMemberCount: Int?,
      @Argument
      @Min(ACTIVITY_WINDOW_MINUTES_LOWER_BOUND)
      @Max(ACTIVITY_WINDOW_MINUTES_UPPER_BOUND)
      activityWindowMinutes: Int?
  ): AdminActiveRoomStats {
    return membershipServiceImpl.findAdminActiveRoomStats(minMemberCount, activityWindowMinutes)
  }

  @QueryMapping
  fun adminRoomActivityStats(
      @Argument from: Instant,
      @Argument to: Instant
  ): AdminRoomActivityStats {
    val managingUserCount = roomService.countAllCreatorsByCreatedAtRange(from, to)
    val participantCount =
        membershipServiceImpl.countAllParticipantUsersByLastActivityAtRange(from, to)
    val roomCount =
        roomService.countByAuditMetadataCreatedAtGreaterThanEqualAndAuditMetadataCreatedAtLessThan(
            from, to)
    return AdminRoomActivityStats(
        managingUserCount = managingUserCount,
        participantCount = participantCount,
        roomCount = roomCount)
  }

  @QueryMapping
  fun adminRoomStats(): AdminRoomStats {
    return AdminRoomStats(
        totalCount = roomService.count(),
        activeRoomCount = membershipServiceImpl.countAllActiveRooms(),
        membershipCount = membershipServiceImpl.count(),
        managingUserCount = membershipServiceImpl.countAllManagingUsers(),
        participantCount = membershipServiceImpl.countAllParticipantUsers(),
    )
  }
}
