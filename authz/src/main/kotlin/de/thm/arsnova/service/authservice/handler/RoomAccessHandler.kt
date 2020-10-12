package de.thm.arsnova.service.authservice.handler

import de.thm.arsnova.service.authservice.config.RabbitConfig
import de.thm.arsnova.service.authservice.model.RoomAccess
import de.thm.arsnova.service.authservice.model.RoomAccessPK
import de.thm.arsnova.service.authservice.model.RoomAccessSyncTracker
import de.thm.arsnova.service.authservice.model.command.CreateRoomAccessCommand
import de.thm.arsnova.service.authservice.model.command.DeleteRoomAccessCommand
import de.thm.arsnova.service.authservice.model.command.RequestRoomAccessSyncCommand
import de.thm.arsnova.service.authservice.model.command.SyncRoomAccessCommand
import de.thm.arsnova.service.authservice.model.event.RoomAccessSyncRequest
import de.thm.arsnova.service.authservice.persistence.RoomAccessRepository
import de.thm.arsnova.service.authservice.persistence.RoomAccessSyncTrackerRepository
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
import java.util.Optional

@Component
class RoomAccessHandler (
        private val rabbitTemplate: RabbitTemplate,
        private val roomAccessRepository: RoomAccessRepository,
        private val roomAccessSyncTrackerRepository: RoomAccessSyncTrackerRepository
) {
    companion object {
        const val ROLE_CREATOR_STRING = "CREATOR"
    }

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    @Transactional(isolation = Isolation.SERIALIZABLE)
    @Retryable(value = [CannotAcquireLockException::class], maxAttempts = 3, backoff = Backoff(delay = 1000))
    fun handleCreateRoomAccessCommand(command: CreateRoomAccessCommand) {
        logger.debug("Handling command: {}", command)
        val syncTracker = roomAccessSyncTrackerRepository.findById(command.roomId)
                .orElse(RoomAccessSyncTracker(command.roomId, "0"))
        // Migration step: Always make sure to not interfere with running synchronisations and unordered events
        if (syncTracker.rev.substringBefore("-").toInt() < command.rev.substringBefore("-").toInt()) {
            // newer information
            try {
                roomAccessRepository.save(RoomAccess(command.roomId, command.userId, command.rev, command.role))
            } catch (e: Exception) {
                logger.error(e.toString())
            }
        } else {
            logger.info("Got older information, command rev: {}, tracker rev: {}", command.rev, syncTracker.rev)
        }
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    @Retryable(value = [CannotAcquireLockException::class], maxAttempts = 3, backoff = Backoff(delay = 1000))
    fun handleDeleteRoomAccessCommand(command: DeleteRoomAccessCommand) {
        logger.debug("Handling command: {}", command)
        val syncTracker = roomAccessSyncTrackerRepository.findById(command.roomId)
                .orElse(RoomAccessSyncTracker(command.roomId, "0"))
        // Migration step: Always make sure to not interfere with running synchronisations and unordered events
        if (syncTracker.rev.substringBefore("-").toInt() - 1 < command.rev.substringBefore("-").toInt()) {
            // newer information
            try {
                roomAccessRepository.deleteById(RoomAccessPK(command.roomId, command.userId))
            } catch (emptyResultDataAccessException: EmptyResultDataAccessException) {
                logger.debug("No room access entry found for: {}", RoomAccessPK(command.roomId, command.userId))
            } catch (e: Exception) {
                logger.error(e.toString())
            }
        } else {
            logger.info("Got older information, command rev: {}, tracker rev: {}", command.rev, syncTracker.rev)
        }
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    @Retryable(value = [CannotAcquireLockException::class], maxAttempts = 3, backoff = Backoff(delay = 1000))
    fun handleRequestRoomAccessSyncCommand(command: RequestRoomAccessSyncCommand): RoomAccessSyncTracker {
        logger.debug("Handling request: {}", command)
        val syncTracker = roomAccessSyncTrackerRepository.findById(command.roomId)
                .orElse(RoomAccessSyncTracker(command.roomId, "0"))
        if (syncTracker.rev.substringBefore("-").toInt() < command.revNumber) {
            // if either no sync has happened or the sync is older than the rev the request aims for
            val newTracker = RoomAccessSyncTracker(command.roomId, "0");
            logger.debug("Saving tracker to indicate sync process: {}", newTracker)
            roomAccessSyncTrackerRepository.save(newTracker)
            val event = RoomAccessSyncRequest(command.roomId)
            logger.debug("Sending room access sync request: {}", event)
            rabbitTemplate.convertAndSend(
                    RabbitConfig.roomAccessSyncRequestQueueName,
                    event
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
        val syncTracker = roomAccessSyncTrackerRepository.findById(command.roomId)
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
            val toDelete = allRoomAccess.filter { i ->
                i.rev.substringBefore("-").toInt() < command.rev.substringBefore("-").toInt()
            }

            logger.debug("Deleting room access: {}", toDelete)
            roomAccessRepository.deleteAll(toDelete)

            val newAccess = command.access.map { i ->
                RoomAccess(command.roomId, i.userId, command.rev, i.role)
            }

            logger.debug("Saving new access: {}", newAccess)
            roomAccessRepository.saveAll(newAccess)

            syncTracker.rev = command.rev
            logger.error("{}", syncTracker)
            logger.error("{}", roomAccessSyncTrackerRepository)
            roomAccessSyncTrackerRepository.save(syncTracker)
        }
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    @Retryable(value = [CannotAcquireLockException::class], maxAttempts = 3, backoff = Backoff(delay = 1000))
    fun getByRoomIdAndUserId(roomId: String, userId: String): Optional<RoomAccess> {
        logger.debug("Handling room access request with roomId: {} and userId: {}", roomId, userId)
        val syncTracker = roomAccessSyncTrackerRepository.findById(roomId)
        // Migration step: always synchronise at least once
        if (!syncTracker.isPresent) {
            logger.trace("Starting room sync for roomId: {}", roomId)
            val newTracker = RoomAccessSyncTracker(roomId, "0");
            logger.debug("Saving tracker to indicate sync process: {}", newTracker)
            roomAccessSyncTrackerRepository.save(newTracker)
            val event = RoomAccessSyncRequest(roomId)
            logger.debug("Sending room access sync request: {}", event)
            rabbitTemplate.convertAndSend(
                    RabbitConfig.roomAccessSyncRequestQueueName,
                    event
            )
            return Optional.empty()
        } else {
            return roomAccessRepository.findById(RoomAccessPK(roomId, userId))
        }
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    @Retryable(value = [CannotAcquireLockException::class], maxAttempts = 3, backoff = Backoff(delay = 1000))
    fun getByRoomId(roomId: String): List<RoomAccess> {
        logger.debug("Handling room access request with roomId: {}", roomId)
        val syncTracker = roomAccessSyncTrackerRepository.findById(roomId)
        // Migration step: always synchronise at least once
        if (!syncTracker.isPresent) {
            logger.trace("Starting room sync for roomId: {}", roomId)
            val newTracker = RoomAccessSyncTracker(roomId, "0");
            logger.debug("Saving tracker to indicate sync process: {}", newTracker)
            roomAccessSyncTrackerRepository.save(newTracker)
            val event = RoomAccessSyncRequest(roomId)
            logger.debug("Sending room access sync request: {}", event)
            rabbitTemplate.convertAndSend(
                RabbitConfig.roomAccessSyncRequestQueueName,
                event
            )
        }
        return roomAccessRepository.findByRoomId(roomId).toList()
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    @Retryable(value = [CannotAcquireLockException::class], maxAttempts = 3, backoff = Backoff(delay = 1000))
    fun getOwnerRoomAccessByRoomId(roomId: String): RoomAccess? {
        logger.debug("Handling room access request with roomId: {}", roomId)
        val syncTracker = roomAccessSyncTrackerRepository.findById(roomId)
        // Migration step: always synchronise at least once
        if (!syncTracker.isPresent) {
            logger.trace("Starting room sync for roomId: {}", roomId)
            val newTracker = RoomAccessSyncTracker(roomId, "0");
            logger.debug("Saving tracker to indicate sync process: {}", newTracker)
            roomAccessSyncTrackerRepository.save(newTracker)
            val event = RoomAccessSyncRequest(roomId)
            logger.debug("Sending room access sync request: {}", event)
            rabbitTemplate.convertAndSend(
                RabbitConfig.roomAccessSyncRequestQueueName,
                event
            )
        }
        return roomAccessRepository.findByRoomIdAndRole(roomId, ROLE_CREATOR_STRING).firstOrNull()
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    @Retryable(value = [CannotAcquireLockException::class], maxAttempts = 3, backoff = Backoff(delay = 1000))
    fun getByUserId(userId: String): Iterable<RoomAccess> {
        logger.debug("Handling room access request by userId: {}", userId)
        return roomAccessRepository.findByUserId(userId)
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    @Retryable(value = [CannotAcquireLockException::class], maxAttempts = 3, backoff = Backoff(delay = 1000))
    fun getByPK(pk: RoomAccessPK): Optional<RoomAccess> {
        return roomAccessRepository.findById(pk)
    }
}
