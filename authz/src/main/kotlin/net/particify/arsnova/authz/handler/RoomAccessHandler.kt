package net.particify.arsnova.authz.handler

import net.particify.arsnova.authz.config.RabbitConfig
import net.particify.arsnova.authz.exception.ForbiddenException
import net.particify.arsnova.authz.exception.InternalServerErrorException
import net.particify.arsnova.authz.model.RoomAccess
import net.particify.arsnova.authz.model.RoomAccessPK
import net.particify.arsnova.authz.model.RoomAccessSyncTracker
import net.particify.arsnova.authz.model.command.RequestRoomAccessSyncCommand
import net.particify.arsnova.authz.model.command.SyncRoomAccessCommand
import net.particify.arsnova.authz.model.event.RoomAccessSyncRequest
import net.particify.arsnova.authz.persistence.RoomAccessRepository
import net.particify.arsnova.authz.persistence.RoomAccessSyncTrackerRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.dao.CannotAcquireLockException
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional
import java.util.Date
import java.util.Optional
import java.util.UUID

@Component
class RoomAccessHandler(
  private val rabbitTemplate: RabbitTemplate,
  private val roomAccessRepository: RoomAccessRepository,
  private val roomAccessSyncTrackerRepository: RoomAccessSyncTrackerRepository,
) {
  companion object {
    const val ROLE_OWNER_STRING = "OWNER"
    const val ROLE_PARTICIPANT_STRING = "PARTICIPANT"
  }

  private val logger: Logger = LoggerFactory.getLogger(javaClass)

  @Transactional(isolation = Isolation.SERIALIZABLE)
  @Retryable(value = [CannotAcquireLockException::class], maxAttempts = 3, backoff = Backoff(delay = 1000))
  fun handleRequestRoomAccessSyncCommand(command: RequestRoomAccessSyncCommand): RoomAccessSyncTracker {
    logger.debug("Handling request: {}", command)
    val syncTracker =
      roomAccessSyncTrackerRepository.findById(command.roomId)
        .orElse(RoomAccessSyncTracker(command.roomId, "0"))
    if (syncTracker.rev.substringBefore("-").toInt() < command.revNumber) {
      // if either no sync has happened or the sync is older than the rev the request aims for
      val newTracker = RoomAccessSyncTracker(command.roomId, "0")
      logger.debug("Saving tracker to indicate sync process: {}", newTracker)
      roomAccessSyncTrackerRepository.save(newTracker)
      val event = RoomAccessSyncRequest(command.roomId)
      logger.debug("Sending room access sync request: {}", event)
      rabbitTemplate.convertAndSend(
        RabbitConfig.ROOM_ACCESS_SYNC_REQUEST_QUEUE_NAME,
        event,
      )
      return newTracker
    } else {
      // Data is already up to given rev
      return syncTracker
    }
  }

  @Transactional(isolation = Isolation.SERIALIZABLE)
  @Retryable(value = [CannotAcquireLockException::class], maxAttempts = 3, backoff = Backoff(delay = 1000))
  fun handleSyncRoomAccessCommand(command: SyncRoomAccessCommand) {
    logger.debug("Handling event: {}", command)
    val syncTracker =
      roomAccessSyncTrackerRepository.findById(command.roomId)
        .orElse(RoomAccessSyncTracker(command.roomId, "0"))
    // Check if event has newer information based on the revision
    if (syncTracker.rev.substringBefore("-").toInt() > command.rev.substringBefore("-").toInt()) {
      // This should not happen but may be because of asynchronicity, especially with multiple instances
      logger.error("Got older information, command rev: {}, tracker rev: {}", command.rev, syncTracker.rev)
    } else if (syncTracker.rev.substringBefore("-").toInt() == command.rev.substringBefore("-").toInt()) {
      // Shouldn't happen to often, is definitely worth monitoring because it might be because of many clients
      // simultaneously needing auth information (gateways)
      logger.info("Got information from the same revision: {}", command.rev)
    } else {
      val allRoomAccess: Iterable<RoomAccess> = roomAccessRepository.findByRoomId(command.roomId)
      val toDelete =
        allRoomAccess.filter { i ->
          i.rev.substringBefore("-").toInt() < command.rev.substringBefore("-").toInt()
        }

      logger.debug("Deleting room access: {}", toDelete)
      roomAccessRepository.deleteAll(toDelete)

      val newAccess =
        command.access.map { i ->
          RoomAccess(command.roomId, i.userId, command.rev, i.role, null, null)
        }

      logger.debug("Saving new access: {}", newAccess)
      roomAccessRepository.saveAll(newAccess)

      syncTracker.rev = command.rev
      logger.error("{}", syncTracker)
      logger.error("{}", roomAccessSyncTrackerRepository)
      roomAccessSyncTrackerRepository.save(syncTracker)
    }
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  @Retryable(value = [CannotAcquireLockException::class], maxAttempts = 3, backoff = Backoff(delay = 1000))
  fun getByRoomIdAndUserId(
    roomId: UUID,
    userId: UUID,
  ): Optional<RoomAccess> {
    logger.debug("Handling room access request with roomId: {} and userId: {}", roomId, userId)
    val lastAccess = Date()
    return roomAccessRepository.updateLastAccessAndGetByRoomIdAndUserId(roomId, userId, lastAccess)
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  @Retryable(value = [CannotAcquireLockException::class], maxAttempts = 3, backoff = Backoff(delay = 1000))
  fun getByRoomIdAndRole(
    roomId: UUID,
    roleParam: String?,
  ): List<RoomAccess> {
    logger.debug("Handling room access request with roomId: {}", roomId)
    if (roleParam != null) {
      if (roleParam.startsWith("!")) {
        return roomAccessRepository.findByRoomIdAndRoleNot(roomId, roleParam.drop(1)).toList()
      }
      return roomAccessRepository.findByRoomIdAndRole(roomId, roleParam).toList()
    }
    return roomAccessRepository.findByRoomId(roomId).toList()
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  @Retryable(value = [CannotAcquireLockException::class], maxAttempts = 3, backoff = Backoff(delay = 1000))
  fun getOwnerRoomAccessByRoomId(roomId: UUID): RoomAccess? {
    logger.debug("Handling room access request with roomId: {}", roomId)
    return roomAccessRepository.findByRoomIdAndRole(roomId, ROLE_OWNER_STRING).firstOrNull()
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  @Retryable(value = [CannotAcquireLockException::class], maxAttempts = 3, backoff = Backoff(delay = 1000))
  fun getByUserId(userId: UUID): Iterable<RoomAccess> {
    logger.debug("Handling room access request by userId: {}", userId)
    return roomAccessRepository.findByUserId(userId)
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  @Retryable(value = [CannotAcquireLockException::class], maxAttempts = 3, backoff = Backoff(delay = 1000))
  fun getByPK(pk: RoomAccessPK): Optional<RoomAccess> {
    return roomAccessRepository.findById(pk)
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  @Retryable(value = [CannotAcquireLockException::class], maxAttempts = 3, backoff = Backoff(delay = 1000))
  fun getUserIdsLastActiveBefore(lastActiveBefore: Date): Iterable<UUID> {
    return roomAccessRepository.findUserIdsByLastAccessBefore(lastActiveBefore)
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  @Retryable(value = [CannotAcquireLockException::class], maxAttempts = 3, backoff = Backoff(delay = 1000))
  fun create(roomAccess: RoomAccess): RoomAccess {
    return try {
      if (roomAccess.role == ROLE_OWNER_STRING) {
        val existingOwnerPks =
          roomAccessRepository
            .findByRoomIdAndRole(roomAccess.roomId, ROLE_OWNER_STRING).map { roomAccess ->
              RoomAccessPK(roomAccess.roomId, roomAccess.userId)
            }
        val otherOwnerPks = existingOwnerPks.filter { pk -> pk.userId != roomAccess.userId }
        roomAccessRepository.deleteAllById(otherOwnerPks)
      }
      roomAccessRepository.createOrUpdateAccess(
        roomAccess.roomId,
        roomAccess.userId,
        roomAccess.rev,
        roomAccess.role!!,
        roomAccess.role!!,
      )
    } catch (ex: EmptyResultDataAccessException) {
      logger.debug("Could not extract result set, most likely due to an already existing creator room access")
      // Updating lastAccess and returning the existing creator room access
      // I don't know of any scenario where there would not be an existing creator role
      val lastAccess = Date()
      roomAccessRepository
        .updateLastAccessAndGetByRoomIdAndUserId(roomAccess.roomId, roomAccess.userId, lastAccess)
        .orElseThrow {
          InternalServerErrorException("Tried fetching the room access but could not obtain any")
        }
    }
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  @Retryable(value = [CannotAcquireLockException::class], maxAttempts = 3, backoff = Backoff(delay = 1000))
  fun createParticipantAccessWithLimit(
    roomAccess: RoomAccess,
    limit: Int,
  ): RoomAccess {
    if (roomAccessRepository.countByRoomIdAndRole(roomAccess.roomId, ROLE_PARTICIPANT_STRING) < limit) {
      return roomAccessRepository.createParticipantAccess(
        roomAccess.roomId,
        roomAccess.userId,
        roomAccess.rev,
      )
    } else {
      throw ForbiddenException("Participant limit reached")
    }
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  @Retryable(value = [CannotAcquireLockException::class], maxAttempts = 3, backoff = Backoff(delay = 1000))
  fun migrateParticipantAccess(
    userId: UUID,
    roomIds: List<UUID>,
  ) {
    val newRoomAccessList: List<RoomAccess> =
      roomIds.map { roomId ->
        roomAccessRepository.createParticipantAccess(
          roomId,
          userId,
          "0-0",
        )
      }
    logger.debug("Migrated participant room access: {}", newRoomAccessList)
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  fun delete(
    roomId: UUID,
    userId: UUID,
  ) {
    roomAccessRepository.deleteByRoomIdAndUserIdWithoutChecking(roomId, userId)
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  fun deleteByRoomId(roomId: UUID): List<RoomAccess> {
    return roomAccessRepository.deleteByRoomIdWithoutChecking(roomId).toList()
  }
}
