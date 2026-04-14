/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.room.internal.api

import com.querydsl.core.BooleanBuilder
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.Locale
import java.util.UUID
import net.particify.arsnova.common.uuid.UuidHelper
import net.particify.arsnova.core4.room.Membership
import net.particify.arsnova.core4.room.QMembership
import net.particify.arsnova.core4.room.QRoom
import net.particify.arsnova.core4.room.Room
import net.particify.arsnova.core4.room.RoomRole
import net.particify.arsnova.core4.room.exception.RoomNotFoundException
import net.particify.arsnova.core4.room.internal.MembershipServiceImpl
import net.particify.arsnova.core4.room.internal.RoomServiceImpl
import org.springframework.data.domain.ScrollPosition
import org.springframework.data.domain.Sort
import org.springframework.data.domain.Window
import org.springframework.data.repository.findByIdOrNull
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.graphql.data.query.ScrollSubrange
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

@Controller
@PreAuthorize("hasRole('ADMIN')")
@SchemaMapping(typeName = "Query")
class AdminRoomQueryController(
    private val roomService: RoomServiceImpl,
    private val membershipService: MembershipServiceImpl
) {
  companion object {
    const val DEFAULT_QUERY_LIMIT = 50
  }

  @QueryMapping
  fun adminRoomById(@Argument id: UUID): Room {
    return this.roomService.findByIdOrNull(id) ?: throw RoomNotFoundException(id)
  }

  @QueryMapping
  fun adminRooms(@Argument search: String?, subrange: ScrollSubrange): Window<Room> {
    val queryBuilder = BooleanBuilder()
    val queryFilter = BooleanBuilder()
    val qRoom = QRoom.room
    if (search != null) {
      val id =
          try {
            UuidHelper.stringToUuid(search)
          } catch (_: IllegalArgumentException) {
            null
          }
      if (id != null) {
        queryFilter.or(qRoom.id.eq(id))
      } else {
        queryFilter.or(qRoom.name.containsIgnoreCase(search))
        queryFilter.or(qRoom.shortId.stringValue().contains(search))
      }
      queryBuilder.or(queryFilter)
    }
    return roomService.findBy(queryBuilder) { q ->
      q.sortBy(Sort.by("auditMetadata.createdAt").descending())
          .limit(DEFAULT_QUERY_LIMIT)
          .scroll(subrange.position().orElse(ScrollPosition.offset()))
    }
  }

  @SchemaMapping(typeName = "AdminRoom")
  fun createdBy(room: Room): UUID? {
    return room.auditMetadata.createdBy
  }

  @SchemaMapping(typeName = "AdminRoom")
  fun createdAt(room: Room): OffsetDateTime? {
    return room.auditMetadata.createdAt?.atOffset(ZoneOffset.UTC)
  }

  @SchemaMapping(typeName = "AdminRoom")
  fun updatedBy(room: Room): UUID? {
    return room.auditMetadata.updatedBy
  }

  @SchemaMapping(typeName = "AdminRoom")
  fun updatedAt(room: Room): OffsetDateTime? {
    return room.auditMetadata.updatedAt?.atOffset(ZoneOffset.UTC)
  }

  @QueryMapping
  fun adminRoomMembershipsByUserId(
      @Argument userId: UUID,
      subrange: ScrollSubrange,
  ): Window<Membership> {
    val queryBuilder = BooleanBuilder()
    queryBuilder.and(QMembership.membership.user.id.eq(userId))
    return membershipService.findBy(queryBuilder) { q ->
      q.sortBy(Sort.by("lastActivityAt").descending())
          .limit(DEFAULT_QUERY_LIMIT)
          .scroll(subrange.position().orElse(ScrollPosition.offset()))
    }
  }

  @QueryMapping
  fun adminRoomManagingMembersByRoomId(@Argument roomId: UUID): List<Membership> {
    val query =
        QMembership.membership.room.id
            .eq(roomId)
            .and(QMembership.membership.role.ne(RoomRole.PARTICIPANT))
    return membershipService.findBy(query) { it.all() }
  }

  @SchemaMapping(typeName = "AdminRoom")
  fun shortId(room: Room): String {
    return String.format(Locale.ROOT, "%0${Room.SHORT_ID_LENGTH}d", room.shortId)
  }
}
