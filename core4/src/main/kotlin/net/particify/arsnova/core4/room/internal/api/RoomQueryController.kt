/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.room.internal.api

import com.querydsl.core.BooleanBuilder
import java.util.Locale
import java.util.UUID
import net.particify.arsnova.core4.common.TextRenderingService
import net.particify.arsnova.core4.room.Membership
import net.particify.arsnova.core4.room.QMembership
import net.particify.arsnova.core4.room.Room
import net.particify.arsnova.core4.room.RoomRole
import net.particify.arsnova.core4.room.exception.MembershipNotFoundException
import net.particify.arsnova.core4.room.exception.RoomNotFoundException
import net.particify.arsnova.core4.room.internal.MembershipServiceImpl
import net.particify.arsnova.core4.room.internal.RoomServiceImpl
import net.particify.arsnova.core4.user.User
import org.springframework.data.domain.Example
import org.springframework.data.domain.ExampleMatcher
import org.springframework.data.domain.ScrollPosition
import org.springframework.data.domain.Sort
import org.springframework.data.domain.Window
import org.springframework.data.repository.findByIdOrNull
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.BatchMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.graphql.data.query.ScrollSubrange
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller

@Controller
@PreAuthorize("hasRole('USER')")
@SchemaMapping(typeName = "Query")
class RoomQueryController(
    private val roomService: RoomServiceImpl,
    private val membershipService: MembershipServiceImpl,
    private val textRenderingService: TextRenderingService
) {
  companion object {
    const val DEFAULT_QUERY_LIMIT = 10
  }

  @QueryMapping
  @PreAuthorize("hasPermission(#id, 'Room', 'read')")
  fun roomById(@Argument id: UUID): Room {
    return roomService.findByIdOrNull(id) ?: throw RoomNotFoundException(id)
  }

  @QueryMapping
  @PreAuthorize("hasPermission(#room.id, 'Room', 'read')")
  fun rooms(@Argument room: Room, subrange: ScrollSubrange): Window<Room> {
    val matcher = ExampleMatcher.matchingAll().withIgnorePaths("version", "description")
    return roomService.findBy(Example.of(room, matcher)) { q ->
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
    if (query?.role != null) {
      queryBuilder.and(QMembership.membership.role.eq(query.role))
    }
    return membershipService.findBy(queryBuilder) { q ->
      q.sortBy(Sort.by("lastActivityAt").descending())
          .limit(DEFAULT_QUERY_LIMIT)
          .scroll(subrange.position().orElse(ScrollPosition.offset()))
    }
  }

  @QueryMapping
  @PreAuthorize("hasPermission(#shortId, 'Room', 'read')")
  fun roomByShortId(@Argument shortId: String): Room {
    return roomService.findOneByShortId(shortId.toInt()) ?: throw RoomNotFoundException()
  }

  @QueryMapping
  @PreAuthorize("hasPermission(#shortId, 'Room', 'read')")
  fun roomMembershipByShortId(
      @Argument shortId: String,
      @AuthenticationPrincipal user: User
  ): Membership {
    return membershipService.findOneByUserIdAndRoomShortId(user?.id!!, shortId.toInt())
        ?: throw MembershipNotFoundException()
  }

  @QueryMapping
  @PreAuthorize("hasPermission(#roomId, 'Room', 'administer')")
  fun roomManagingMembersByRoomId(@Argument roomId: UUID): List<Membership> {
    val query =
        QMembership.membership.room.id
            .eq(roomId)
            .and(QMembership.membership.role.ne(RoomRole.PARTICIPANT))
    return membershipService.findBy(query) { it.all() }
  }

  @QueryMapping
  @PreAuthorize("denyAll")
  fun roomsByUserId(@Argument userId: UUID, subrange: ScrollSubrange): Window<Membership> {
    return membershipService.findByUserId(
        userId, subrange.position().orElse(ScrollPosition.offset()))
  }

  @SchemaMapping(typeName = "Room")
  fun shortId(room: Room): String {
    return String.format(Locale.ROOT, "%0${Room.SHORT_ID_LENGTH}d", room.shortId)
  }

  @SchemaMapping(typeName = "Room", field = "descriptionRendered")
  fun descriptionRendered(room: Room): String? {
    return textRenderingService.renderText(
        room.description,
        TextRenderingService.TextRenderingOptions(
            true,
            true,
            true,
            TextRenderingService.TextRenderingOptions.MarkdownFeatureset.EXTENDED))
  }

  @SchemaMapping(typeName = "Room") fun stats(room: Room) = RoomStats(room.id!!)

  @BatchMapping(typeName = "RoomStats")
  fun activeMemberCount(roomStatsList: List<RoomStats>): Map<RoomStats, Int> {
    val counts = membershipService.countActiveMembersByRoomIds(roomStatsList.map { it.id })
    return roomStatsList.associateWith { counts.getOrDefault(it.id, 0) }
  }

  data class RoomStats(val id: UUID, val activeMemberCount: Int = 0)
}
