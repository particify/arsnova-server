/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.questionnaire.round

import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository

interface RoundRepository : JpaRepository<Round, UUID> {
  fun findByContentIdOrderByRoundNo(contentId: UUID): List<Round>
}
