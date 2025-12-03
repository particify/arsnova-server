/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.room.internal

import jakarta.persistence.criteria.Path
import java.util.UUID
import net.particify.arsnova.core4.room.Membership
import org.apache.catalina.User
import org.springframework.data.domain.ScrollPosition
import org.springframework.data.domain.Window
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.querydsl.QuerydslPredicateExecutor
import org.springframework.graphql.data.GraphQlRepository

@GraphQlRepository
interface MembershipRepository :
    JpaRepository<Membership, Membership.RoomUserId>,
    QuerydslPredicateExecutor<Membership>,
    JpaSpecificationExecutor<Membership> {
  companion object {
    fun hasUserId(userId: UUID): Specification<Membership> {
      return Specification<Membership> { root, _, cb ->
        val roomUser = root.join<User, Membership>("user")
        cb.equal(roomUser.get<Path<UUID>>("userId"), userId)
      }
    }
  }

  fun findOneByUserIdAndRoomShortId(userId: UUID, shortId: Int): Membership?

  fun findByUserId(userId: UUID, scrollPosition: ScrollPosition): Window<Membership>

  fun findOneByRoomIdAndUserId(roomId: UUID, userId: UUID): Membership?
}
