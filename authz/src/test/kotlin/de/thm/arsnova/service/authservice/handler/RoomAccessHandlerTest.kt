package de.thm.arsnova.service.authservice.handler

import de.thm.arsnova.service.authservice.model.RoomAccess
import de.thm.arsnova.service.authservice.model.RoomAccessEntry
import de.thm.arsnova.service.authservice.model.RoomAccessPK
import de.thm.arsnova.service.authservice.model.RoomAccessSyncTracker
import de.thm.arsnova.service.authservice.model.command.CreateRoomAccessCommand
import de.thm.arsnova.service.authservice.model.command.DeleteRoomAccessCommand
import de.thm.arsnova.service.authservice.model.command.RequestRoomAccessSyncCommand
import de.thm.arsnova.service.authservice.model.command.SyncRoomAccessCommand
import de.thm.arsnova.service.authservice.model.event.RoomAccessSyncRequest
import de.thm.arsnova.service.authservice.persistence.RoomAccessRepository
import de.thm.arsnova.service.authservice.persistence.RoomAccessSyncTrackerRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.amqp.rabbit.core.RabbitTemplate
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class RoomAccessHandlerTest {
    @Mock private lateinit var rabbitTemplate: RabbitTemplate
    @Mock private lateinit var roomAccessRepository: RoomAccessRepository
    @Mock private lateinit var roomAccessSyncTrackerRepository: RoomAccessSyncTrackerRepository
    private lateinit var roomAccessHandler: RoomAccessHandler

    val SOME_ROOM_ID = "23e7c082c533e49963b96d7a0500105f"
    val SOME_REV = "1-93b09a4699769e6abd3bf5b2ff341e5d"
    val SOME_NEWER_REV = "2-93b09a4699769e6abd3bf5b2ff341e5e"
    val SOME_EVEN_NEWER_REV = "3-93b09a4699769e6abd3bf5b2ff341e5f"
    val SOME_USER_ID = "23e7c082c533e49963b96d7a05000d0e"
    val SOME_OTHER_USER_ID = "aaaac082c533e49963b96d7a05000d0f"
    val SOME_MODERATOR_ID = "bbbbc082c533e49963b96d7a05000d0f"
    val CREATOR_STRING = "CREATOR"
    val EXECUTIVE_MODERATOR_STRING = "EXECUTIVE_MODERATOR"

    @BeforeEach
    fun setUp() {
        roomAccessHandler =
                RoomAccessHandler(rabbitTemplate, roomAccessRepository, roomAccessSyncTrackerRepository)
    }

    @Test
    fun testHandleCreateRoomAccessCommand() {
        val command = CreateRoomAccessCommand(
                SOME_REV,
                SOME_ROOM_ID,
                SOME_USER_ID,
                CREATOR_STRING
        )
        val expected = RoomAccess(
                SOME_ROOM_ID,
                SOME_USER_ID,
                SOME_REV,
                CREATOR_STRING
        )

        Mockito.`when`(roomAccessSyncTrackerRepository.findById(command.roomId))
                .thenReturn(Optional.of(RoomAccessSyncTracker(
                        SOME_ROOM_ID,
                        "0"
                )))

        roomAccessHandler.handleCreateRoomAccessCommand(command)

        verify(roomAccessRepository, times(1)).save(expected)
    }

    @Test
    fun testHandleDeleteRoomAccessCommand() {
        val command = DeleteRoomAccessCommand(
                SOME_REV,
                SOME_ROOM_ID,
                SOME_USER_ID
        )
        val expected = RoomAccessPK(
                SOME_ROOM_ID,
                SOME_USER_ID
        )

        Mockito.`when`(roomAccessSyncTrackerRepository.findById(command.roomId))
                .thenReturn(Optional.of(RoomAccessSyncTracker(
                        SOME_ROOM_ID,
                        "0"
                )))

        roomAccessHandler.handleDeleteRoomAccessCommand(command)

        verify(roomAccessRepository, times(1)).deleteById(expected)
    }

    @Test
    fun testStartSyncOnCommand() {
        val command = RequestRoomAccessSyncCommand(
                SOME_ROOM_ID,
                2
        )
        val expected = RoomAccessSyncRequest(
                SOME_ROOM_ID
        )

        Mockito.`when`(roomAccessSyncTrackerRepository.findById(command.roomId))
                .thenReturn(Optional.of(RoomAccessSyncTracker(
                        SOME_ROOM_ID,
                        SOME_REV
                )))
        val keyCaptor = ArgumentCaptor.forClass(String::class.java)
        val eventCaptor = ArgumentCaptor.forClass(RoomAccessSyncRequest::class.java)

        roomAccessHandler.handleRequestRoomAccessSyncCommand(command)

        verify(rabbitTemplate, times(1))
                .convertAndSend(keyCaptor.capture(), eventCaptor.capture())
        assertEquals(keyCaptor.value, "backend.event.room.access.sync.request")
        assertEquals(eventCaptor.value, expected)
    }

    @Test
    fun testHandleSyncRoomAccessCommand() {
        val command = SyncRoomAccessCommand(
                SOME_NEWER_REV,
                SOME_ROOM_ID,
                listOf(
                        RoomAccessEntry(SOME_OTHER_USER_ID, CREATOR_STRING)
                )
        )
        val expectedDelete = RoomAccess(
                SOME_ROOM_ID,
                SOME_USER_ID,
                SOME_REV,
                CREATOR_STRING
        )
        val expectedCreate = RoomAccess(
                SOME_ROOM_ID,
                SOME_OTHER_USER_ID,
                SOME_NEWER_REV,
                CREATOR_STRING
        )
        val expectedTracker = RoomAccessSyncTracker(
                SOME_ROOM_ID,
                SOME_NEWER_REV
        )

        Mockito.`when`(roomAccessSyncTrackerRepository.findById(command.roomId))
                .thenReturn(Optional.of(RoomAccessSyncTracker(
                        SOME_ROOM_ID,
                        SOME_REV
                )))
        Mockito.`when`(roomAccessSyncTrackerRepository.save(expectedTracker))
                .thenReturn(expectedTracker)
        Mockito.`when`(roomAccessRepository.findByRoomId(command.roomId))
                .thenReturn(listOf(
                        // Do not delete this one
                        RoomAccess(
                                SOME_ROOM_ID,
                                SOME_MODERATOR_ID,
                                SOME_EVEN_NEWER_REV,
                                EXECUTIVE_MODERATOR_STRING
                        ),
                        // This is old and should get deleted
                        expectedDelete
                ))

        roomAccessHandler.handleSyncRoomAccessCommand(command)

        verify(roomAccessRepository, times(1)).deleteAll(listOf(expectedDelete))
        verify(roomAccessRepository, times(1)).saveAll(listOf(expectedCreate))
        verify(roomAccessSyncTrackerRepository, times(1)).save(expectedTracker)
    }

    @Test
    fun testDoNotUpdateOnOldInfo() {
        val command = SyncRoomAccessCommand(
                SOME_REV,
                SOME_ROOM_ID,
                listOf(
                        RoomAccessEntry(SOME_USER_ID, CREATOR_STRING)
                )
        )
        val trackerCaptor = ArgumentCaptor.forClass(RoomAccessSyncTracker::class.java)

        verify(roomAccessSyncTrackerRepository, times(0)).save(trackerCaptor.capture())
    }

    @Test
    fun testGetByRoomIdAndUserId() {
        val expectedPK = RoomAccessPK(
                SOME_ROOM_ID,
                SOME_USER_ID
        )

        Mockito.`when`(roomAccessSyncTrackerRepository.findById(SOME_ROOM_ID))
                .thenReturn(Optional.of(RoomAccessSyncTracker(
                        SOME_ROOM_ID,
                        SOME_REV
                )))

        roomAccessHandler.getByRoomIdAndUserId(SOME_ROOM_ID, SOME_USER_ID)

        verify(roomAccessRepository, times(1)).findById(expectedPK)
    }

    @Test
    fun startSyncOnRequestAndMissingTracker() {
        val expected = RoomAccessSyncRequest(
                SOME_ROOM_ID
        )

        Mockito.`when`(roomAccessSyncTrackerRepository.findById(SOME_ROOM_ID))
                .thenReturn(Optional.empty())

        val keyCaptor = ArgumentCaptor.forClass(String::class.java)
        val eventCaptor = ArgumentCaptor.forClass(RoomAccessSyncRequest::class.java)

        roomAccessHandler.getByRoomIdAndUserId(SOME_ROOM_ID, SOME_USER_ID)

        verify(rabbitTemplate, times(1))
                .convertAndSend(keyCaptor.capture(), eventCaptor.capture())
        assertEquals(keyCaptor.value, "backend.event.room.access.sync.request")
        assertEquals(eventCaptor.value, expected)
    }
}
