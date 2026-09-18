/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.room.internal

import jakarta.persistence.Tuple
import java.time.Instant
import java.util.UUID
import net.particify.arsnova.core4.room.Membership
import net.particify.arsnova.core4.room.RoomRole
import org.springframework.data.domain.ScrollPosition
import org.springframework.data.domain.Window
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.querydsl.QuerydslPredicateExecutor

@Suppress("TooManyFunctions")
interface MembershipRepository :
    JpaRepository<Membership, Membership.RoomUserId>, QuerydslPredicateExecutor<Membership> {
  fun findOneByUserIdAndRoomShortId(userId: UUID, shortId: Int): Membership?

  fun findByUserId(userId: UUID, scrollPosition: ScrollPosition): Window<Membership>

  @Query(
      "SELECT m.id " +
          "FROM Membership m " +
          "WHERE m.id.userId = :userId " +
          "AND m.role = 'OWNER'")
  fun findIdsByIdUserIdAndIsOwner(userId: UUID): List<Membership.RoomUserId>

  fun findOneByRoomIdAndUserId(roomId: UUID, userId: UUID): Membership?

  @Query(
      "SELECT m.room.id, COUNT(*) FROM Membership m " +
          "WHERE m.room.id IN :roomIds AND m.lastActivityAt > :lastActivityAtAfter GROUP BY m.room")
  fun countByRoomIdsAndLastActivityAtAfter(
      roomIds: List<UUID>,
      lastActivityAtAfter: Instant
  ): List<Tuple>

  fun findOneByRoomIdAndRole(roomId: UUID, role: RoomRole): Membership?

  fun deleteAllByIdUserId(userId: UUID): Int

  @Query(
      "SELECT COUNT(roomId) FROM (" +
          "SELECT m.room.id roomId FROM Membership m " +
          "WHERE m.lastActivityAt > :lastActivityAtAfter " +
          "GROUP BY m.room " +
          "HAVING COUNT(m) >= :minMemberCount)")
  fun countAllActiveRoomsAndLastActivityAtAfter(
      lastActivityAtAfter: Instant,
      minMemberCount: Int
  ): Long

  @Query("SELECT COUNT(DISTINCT m.user) FROM Membership m " + "WHERE m.role != 'PARTICIPANT'")
  fun countAllManagingUsers(): Long

  @Query("SELECT COUNT(DISTINCT m.user) FROM Membership m " + "WHERE m.role = 'PARTICIPANT'")
  fun countAllParticipantUsers(): Long

  @Query(
      "SELECT COUNT(DISTINCT m.user) FROM Membership m " +
          "WHERE m.role = 'PARTICIPANT' " +
          "AND m.lastActivityAt >= :from " +
          "AND m.lastActivityAt < :to")
  fun countAllParticipantUsersByLastActivityAtRange(from: Instant, to: Instant): Long

  @Query(
      "SELECT COUNT(DISTINCT m.user) FROM Membership m " +
          "WHERE m.role != 'PARTICIPANT' " +
          "AND m.lastActivityAt >= :from " +
          "AND m.lastActivityAt < :to")
  fun countAllManagingUsersByLastActivityAtRange(from: Instant, to: Instant): Long
}
