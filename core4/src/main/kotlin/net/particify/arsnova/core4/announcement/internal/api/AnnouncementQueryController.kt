/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.announcement.internal.api

import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import net.particify.arsnova.core4.announcement.Announcement
import net.particify.arsnova.core4.announcement.internal.AnnouncementServiceImpl
import net.particify.arsnova.core4.common.TextRenderingService
import net.particify.arsnova.core4.room.MembershipService
import net.particify.arsnova.core4.user.User
import net.particify.arsnova.core4.user.UserService
import org.springframework.data.domain.ScrollPosition
import org.springframework.data.domain.Window
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.graphql.data.query.ScrollSubrange
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller

@Controller
@SchemaMapping(typeName = "Query")
class AnnouncementQueryController(
    private val announcementService: AnnouncementServiceImpl,
    private val membershipService: MembershipService,
    private val userService: UserService,
    private val textRenderingService: TextRenderingService
) {
  @QueryMapping
  fun announcementsByRoomId(
      @Argument roomId: UUID,
      subrange: ScrollSubrange
  ): Window<Announcement> {
    return announcementService.findByRoomIdOrderByAuditMetadataCreatedAtDesc(
        roomId, subrange.position().orElse(ScrollPosition.offset()))
  }

  @QueryMapping
  fun announcementsForCurrentUser(
      @AuthenticationPrincipal user: User,
      subrange: ScrollSubrange
  ): Window<Announcement> {
    return announcementsByUserId(user.id!!, subrange)
  }

  @QueryMapping
  fun announcementsByUserId(
      @Argument userId: UUID,
      subrange: ScrollSubrange
  ): Window<Announcement> {
    val memberships = membershipService.findByUserId(userId, ScrollPosition.offset())
    val roomIds = memberships.map { it.room?.id!! }.toSet()
    val announcements =
        announcementService.findByRoomIdInOrderByAuditMetadataCreatedAtDesc(
            roomIds, subrange.position().orElse(ScrollPosition.offset()))
    userService.markAnnouncementsReadForUserId(userId)
    return announcements
  }

  @QueryMapping
  fun announcementsMetaForCurrentUser(@AuthenticationPrincipal user: User): AnnouncementMeta {
    val memberships = membershipService.findByUserId(user.id!!, ScrollPosition.offset())
    val roomIds = memberships.map { it.room?.id!! }.toSet()
    val count =
        announcementService.countByRoomIdInAndAuditMetadataCreatedAtGreaterThan(
            roomIds, user.announcementsReadAt ?: Instant.EPOCH)
    return AnnouncementMeta(count, user.announcementsReadAt?.atOffset(ZoneOffset.UTC))
  }

  @SchemaMapping(typeName = "Announcement")
  fun createdAt(announcement: Announcement): OffsetDateTime? {
    return announcement.auditMetadata.createdAt?.atOffset(ZoneOffset.UTC)
  }

  @SchemaMapping(typeName = "Announcement")
  fun updatedAt(announcement: Announcement): OffsetDateTime? {
    return announcement.auditMetadata.updatedAt?.atOffset(ZoneOffset.UTC)
  }

  @SchemaMapping(typeName = "Announcement", field = "bodyRendered")
  fun bodyRendered(announcement: Announcement): String? {
    return textRenderingService.renderText(announcement.body)
  }

  data class AnnouncementMeta(val count: Int, val readAt: OffsetDateTime?)
}
