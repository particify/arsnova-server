package de.thm.arsnova.service.authservice.handler

import de.thm.arsnova.service.authservice.model.RoomAccess
import de.thm.arsnova.service.authservice.model.RoomAccessPK
import de.thm.arsnova.service.authservice.model.command.CreateRoomAccessCommand
import de.thm.arsnova.service.authservice.model.command.DeleteRoomAccessCommand
import de.thm.arsnova.service.authservice.persistence.RoomAccessRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.stereotype.Component
import java.util.Optional

@Component
class RoomAccessHandler (
        private val repository: RoomAccessRepository
) {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    fun handleCreateRoomAccessCommand(command: CreateRoomAccessCommand) {
        logger.debug("Handling command: {}", command)
        try {
            repository.save(RoomAccess(command.roomId, command.userId, command.rev, command.role))
        } catch (e: Exception) {
            logger.error(e.toString())
        }
    }

    fun handleDeleteRoomAccessCommand(command: DeleteRoomAccessCommand) {
        logger.debug("Handling command: {}", command)
        try {
            repository.deleteById(RoomAccessPK(command.roomId, command.userId))
        } catch (emptyResultDataAccessException: EmptyResultDataAccessException) {
            logger.debug("No room access entry found for: {}", RoomAccessPK(command.roomId, command.userId))
        } catch (e: Exception) {
            logger.error(e.toString())
        }
    }

    fun getByRoomIdAndUserId(roomId: String, userId: String): Optional<RoomAccess> {
        return repository.findById(RoomAccessPK(roomId, userId))
    }

    fun getByPK(pk: RoomAccessPK): Optional<RoomAccess> {
        return repository.findById(pk)
    }
}
