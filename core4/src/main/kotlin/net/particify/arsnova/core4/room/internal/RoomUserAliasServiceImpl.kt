/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.room.internal

import jakarta.transaction.Transactional
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ThreadLocalRandom
import net.particify.arsnova.core4.common.exception.InvalidInputException
import net.particify.arsnova.core4.room.Room
import net.particify.arsnova.core4.room.RoomUserAlias
import net.particify.arsnova.core4.room.RoomUserAliasService
import net.particify.arsnova.core4.user.User
import org.springframework.stereotype.Service

@Service
class RoomUserAliasServiceImpl(
    private val roomUserAliasRepository: RoomUserAliasRepository,
    private val roomUserAliasGenerator: RoomUserAliasGenerator
) : RoomUserAliasService, RoomUserAliasRepository by roomUserAliasRepository {
  override fun findOneByRoomIdAndUserId(roomId: UUID, userId: UUID): RoomUserAlias? {
    return roomUserAliasRepository.findOneByRoomIdAndUserId(roomId, userId)
  }

  override fun retrieveOrGenerate(roomId: UUID, userId: UUID, locale: Locale): RoomUserAlias {
    val persistedAlias =
        findOneByRoomIdAndUserId(roomId, userId) ?: return roomUserAliasGenerator.generate(locale)
    return localizeIfGenerated(persistedAlias, locale)
  }

  @Transactional
  override fun set(
      roomId: UUID,
      userId: UUID,
      alias: String?,
      seed: Int?,
      locale: Locale
  ): RoomUserAlias {
    if (findOneByRoomIdAndUserId(roomId, userId) != null) {
      throw InvalidInputException("Alias has already been created.")
    }
    if (alias != null && (alias.isBlank() || alias.length > RoomUserAlias.MAX_ALIAS_LENGTH)) {
      throw InvalidInputException(
          "Alias must not be blank or exceed ${RoomUserAlias.MAX_ALIAS_LENGTH} characters.")
    }
    val persistedAlias =
        roomUserAliasRepository.save(
            RoomUserAlias(
                room = Room(id = roomId),
                user = User(id = userId),
                alias = alias,
                seed = seed ?: ThreadLocalRandom.current().nextInt()))
    return localizeIfGenerated(persistedAlias, locale)
  }

  /** Aliases without an explicitly set name are generated from their seed on retrieval. */
  private fun localizeIfGenerated(alias: RoomUserAlias, locale: Locale): RoomUserAlias {
    if (alias.alias != null) {
      return alias
    }
    val generatedAlias = roomUserAliasGenerator.generate(alias.seed!!, locale)
    generatedAlias.id = alias.id
    return generatedAlias
  }
}
