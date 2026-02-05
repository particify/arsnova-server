/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.announcement.internal

import java.time.Instant
import java.util.UUID
import net.particify.arsnova.core4.announcement.Announcement
import org.springframework.data.domain.ScrollPosition
import org.springframework.data.domain.Window
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.querydsl.QuerydslPredicateExecutor

interface AnnouncementRepository :
    JpaRepository<Announcement, UUID>, QuerydslPredicateExecutor<Announcement> {
  fun findByRoomIdOrderByAuditMetadataCreatedAtDesc(
      roomId: UUID,
      scrollPosition: ScrollPosition
  ): Window<Announcement>

  fun findByRoomIdInOrderByAuditMetadataCreatedAtDesc(
      roomIds: Collection<UUID>,
      scrollPosition: ScrollPosition
  ): Window<Announcement>

  fun countByRoomIdInAndAuditMetadataCreatedAtGreaterThan(
      roomIds: Collection<UUID>,
      createdAt: Instant
  ): Int

  fun deleteByRoomId(roomId: UUID): Int
}
