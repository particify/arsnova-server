package de.thm.arsnova.service.authservice.listener

import de.thm.arsnova.service.authservice.handler.RoomAccessHandler
import de.thm.arsnova.service.authservice.model.command.CreateRoomAccessCommand
import de.thm.arsnova.service.authservice.model.command.DeleteRoomAccessCommand
import de.thm.arsnova.service.authservice.model.event.RoomAccessGrantedEvent
import de.thm.arsnova.service.authservice.model.event.RoomAccessRevokedEvent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Service

@Service
class RoomAccessListener (
        private val handler: RoomAccessHandler
) {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    @RabbitListener(queues = ["backend.event.room.access.granted"])
    fun receiveRoomAccessGrantedEvent(event: RoomAccessGrantedEvent) {
        logger.debug("Got event on room access granted queue: {}", event)
        handler.handleCreateRoomAccessCommand(CreateRoomAccessCommand(event.roomId, event.userId, event.role))
    }

    @RabbitListener(queues = ["backend.event.room.access.revoked"])
    fun receiveRoomAccessRevokedEvent(event: RoomAccessRevokedEvent) {
        logger.debug("Got event on room access revoked queue: {}", event)
        handler.handleDeleteRoomAccessCommand(DeleteRoomAccessCommand(event.roomId, event.userId))
    }
}
