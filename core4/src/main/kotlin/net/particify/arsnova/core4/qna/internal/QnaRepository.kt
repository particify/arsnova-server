/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.qna.internal

import java.util.UUID
import net.particify.arsnova.core4.qna.Qna
import org.springframework.data.domain.ScrollPosition
import org.springframework.data.domain.Window
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.graphql.data.GraphQlRepository

@GraphQlRepository
interface QnaRepository : JpaRepository<Qna, UUID> {
  fun findByRoomId(roomId: UUID, scrollPosition: ScrollPosition): Window<Qna>
}
