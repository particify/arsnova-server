package de.thm.arsnova.service.authservice.listener

import de.thm.arsnova.service.authservice.config.RabbitConfig
import de.thm.arsnova.service.authservice.handler.RoomAccessHandler
import de.thm.arsnova.service.authservice.model.command.CreateRoomAccessCommand
import de.thm.arsnova.service.authservice.model.command.DeleteRoomAccessCommand
import de.thm.arsnova.service.authservice.model.command.SyncRoomAccessCommand
import de.thm.arsnova.service.authservice.model.event.RoomAccessGrantedEvent
import de.thm.arsnova.service.authservice.model.event.RoomAccessRevokedEvent
import de.thm.arsnova.service.authservice.model.event.RoomAccessSyncEvent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Service

@Service
class RoomAccessListener (
        private val handler: RoomAccessHandler
) {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    @RabbitListener(queues = [RabbitConfig.roomAccessGrantedQueueName])
    fun receiveRoomAccessGrantedEvent(event: RoomAccessGrantedEvent) {
        logger.debug("Got event on room access granted queue: {}", event)
        handler.handleCreateRoomAccessCommand(CreateRoomAccessCommand(event.rev, event.roomId, event.userId, event.role))
    }

    @RabbitListener(queues = [RabbitConfig.roomAccessRevokedQueueName])
    fun receiveRoomAccessRevokedEvent(event: RoomAccessRevokedEvent) {
        logger.debug("Got event on room access revoked queue: {}", event)
        handler.handleDeleteRoomAccessCommand(DeleteRoomAccessCommand(event.rev, event.roomId, event.userId))
    }

    @RabbitListener(queues = [RabbitConfig.roomAccessSyncResponseQueueName])
    fun receiveRoomAccessSyncResponseEvent(event: RoomAccessSyncEvent) {
        logger.debug("Got event on room access sync response queue: {}", event)
        handler.handleSyncRoomAccessCommand(SyncRoomAccessCommand(event.rev, event.roomId, event.access))
    }
}
