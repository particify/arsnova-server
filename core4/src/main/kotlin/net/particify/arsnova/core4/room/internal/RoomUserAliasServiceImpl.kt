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
import net.particify.arsnova.core4.room.RoomUserAliasProjection
import net.particify.arsnova.core4.room.RoomUserAliasService
import net.particify.arsnova.core4.room.event.RoomUserAliasChangedEvent
import net.particify.arsnova.core4.user.User
import org.springframework.context.ApplicationEventPublisher
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service

private const val ALREADY_CREATED_MESSAGE = "Alias has already been created."

@Service
class RoomUserAliasServiceImpl(
    private val roomUserAliasRepository: RoomUserAliasRepository,
    private val roomUserAliasGenerator: RoomUserAliasGenerator,
    private val applicationEventPublisher: ApplicationEventPublisher
) : RoomUserAliasService, RoomUserAliasRepository by roomUserAliasRepository {
  override fun findOneByRoomIdAndUserId(roomId: UUID, userId: UUID): RoomUserAlias? {
    return roomUserAliasRepository.findOneByRoomIdAndUserId(roomId, userId)
  }

  override fun findAliasMappingsByRoomId(roomId: UUID): Map<UUID, RoomUserAliasProjection> {
    return roomUserAliasRepository.findProjectedByRoomId(roomId).associateBy { it.userId }
  }

  override fun localize(alias: RoomUserAliasProjection, locale: Locale): RoomUserAlias {
    if (alias.alias != null) {
      return RoomUserAlias(id = alias.id, alias = alias.alias, seed = alias.seed)
    }
    val generatedAlias = roomUserAliasGenerator.generate(alias.seed!!, locale)
    generatedAlias.id = alias.id
    return generatedAlias
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
      throw InvalidInputException(ALREADY_CREATED_MESSAGE)
    }
    val persistedAlias =
        store(
            RoomUserAlias(
                room = Room(id = roomId),
                user = User(id = userId),
                alias = alias?.let(::normalizeAlias),
                seed = seed ?: ThreadLocalRandom.current().nextInt()))
    applicationEventPublisher.publishEvent(RoomUserAliasChangedEvent(roomId, userId))
    return localizeIfGenerated(persistedAlias, locale)
  }

  /**
   * Concurrent calls can both pass the check in [set], so the unique constraint on the room and the
   * user has the last word and its violation is reported like the check's.
   */
  private fun store(alias: RoomUserAlias): RoomUserAlias =
      try {
        roomUserAliasRepository.saveAndFlush(alias)
      } catch (e: DataIntegrityViolationException) {
        throw InvalidInputException(ALREADY_CREATED_MESSAGE, e)
      }

  private fun normalizeAlias(alias: String): String {
    val trimmed = alias.trim()
    if (trimmed.isEmpty() || trimmed.length > RoomUserAlias.MAX_ALIAS_LENGTH) {
      throw InvalidInputException(
          "Alias must not be blank or exceed ${RoomUserAlias.MAX_ALIAS_LENGTH} characters.")
    }
    return trimmed
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
