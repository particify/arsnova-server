/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.room.internal.api

import com.querydsl.core.BooleanBuilder
import java.util.Locale
import java.util.UUID
import net.particify.arsnova.core4.room.Membership
import net.particify.arsnova.core4.room.QMembership
import net.particify.arsnova.core4.room.Room
import net.particify.arsnova.core4.room.RoomRole
import net.particify.arsnova.core4.room.internal.MembershipRepository
import net.particify.arsnova.core4.room.internal.RoomRepository
import net.particify.arsnova.core4.user.User
import org.springframework.data.domain.Example
import org.springframework.data.domain.ExampleMatcher
import org.springframework.data.domain.ScrollPosition
import org.springframework.data.domain.Sort
import org.springframework.data.domain.Window
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.graphql.data.query.ScrollSubrange
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller

@Controller
@SchemaMapping(typeName = "Query")
class RoomQueryController(
    private val roomRepository: RoomRepository,
    private val membershipRepository: MembershipRepository,
) {
  companion object {
    const val DEFAULT_QUERY_LIMIT = 10
  }

  @QueryMapping
  fun rooms(@Argument room: Room, subrange: ScrollSubrange): Window<Room> {
    val matcher = ExampleMatcher.matchingAll().withIgnorePaths("version", "description")
    return roomRepository.findBy(Example.of(room, matcher)) { q ->
      q.scroll(subrange.position().orElse(ScrollPosition.offset()))
    }
  }

  @QueryMapping
  fun roomMemberships(
      @Argument query: RoomQueryInput?,
      subrange: ScrollSubrange,
      @AuthenticationPrincipal user: User
  ): Window<Membership> {
    val queryBuilder = BooleanBuilder()
    queryBuilder.and(QMembership.membership.user.id.eq(user.id))
    if (query?.shortId != null) {
      queryBuilder.and(QMembership.membership.room.shortId.eq(query.shortId.toInt()))
    }
    if (query?.name != null) {
      queryBuilder.and(QMembership.membership.room.name.containsIgnoreCase(query.name))
    }
    return membershipRepository.findBy(queryBuilder) { q ->
      q.sortBy(Sort.by("lastActivityAt").descending())
          .limit(DEFAULT_QUERY_LIMIT)
          .scroll(subrange.position().orElse(ScrollPosition.offset()))
    }
  }

  @QueryMapping
  fun roomByShortId(@Argument shortId: String): Room {
    return roomRepository.findOneByShortId(shortId.toInt())
  }

  @QueryMapping
  fun roomMembershipByShortId(
      @Argument shortId: String,
      @AuthenticationPrincipal user: User
  ): Membership {
    return membershipRepository.findOneByUserIdAndRoomShortId(user?.id!!, shortId.toInt())
  }

  @QueryMapping
  fun roomManagingMembersByRoomId(@Argument roomId: UUID): List<Membership> {
    val query =
        QMembership.membership.room.id
            .eq(roomId)
            .and(QMembership.membership.role.ne(RoomRole.PARTICIPANT))
    return membershipRepository.findBy(query) { it.all() }
  }

  @QueryMapping
  fun roomsByUserId(@Argument userId: UUID, subrange: ScrollSubrange): Window<Membership> {
    return membershipRepository.findByUserId(
        userId, subrange.position().orElse(ScrollPosition.offset()))
  }

  @SchemaMapping(typeName = "Room")
  fun shortId(room: Room): String {
    return String.format(Locale.ROOT, "%0${Room.SHORT_ID_LENGTH}d", room.shortId)
  }

  @SchemaMapping(typeName = "RoomMembership", field = "stats")
  fun stats(): RoomStats {
    return RoomStats()
  }

  data class RoomStats(
      val roomUserCount: Int = 0,
      val contentCount: Int = 0,
      val ackCommentCount: Int = 0
  )
}
