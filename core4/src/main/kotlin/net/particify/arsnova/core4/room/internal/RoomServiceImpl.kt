/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.room.internal

import java.util.UUID
import net.particify.arsnova.core4.room.Room
import org.springframework.stereotype.Service

@Service
class RoomServiceImpl(private val roomRepository: RoomRepository) :
    RoomRepository by roomRepository {
  override fun deleteById(id: UUID) {
    roomRepository.deleteById(id)
  }

  @Deprecated(
      "Deprecated by base implementation", replaceWith = ReplaceWith("deleteAllInBatch(entities)"))
  override fun deleteInBatch(entities: Iterable<Room>) = roomRepository.deleteInBatch(entities)
}
