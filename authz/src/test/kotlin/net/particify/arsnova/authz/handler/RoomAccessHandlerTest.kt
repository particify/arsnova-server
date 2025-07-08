package net.particify.arsnova.authz.handler

import net.particify.arsnova.authz.model.RoomAccess
import net.particify.arsnova.authz.model.RoomAccessEntry
import net.particify.arsnova.authz.model.RoomAccessSyncTracker
import net.particify.arsnova.authz.model.command.RequestRoomAccessSyncCommand
import net.particify.arsnova.authz.model.command.SyncRoomAccessCommand
import net.particify.arsnova.authz.model.event.RoomAccessSyncRequest
import net.particify.arsnova.authz.persistence.RoomAccessRepository
import net.particify.arsnova.authz.persistence.RoomAccessSyncTrackerRepository
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
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class RoomAccessHandlerTest {
  companion object {
    val SOME_ROOM_ID = UUID.fromString("23e7c082-c533-e499-63b9-6d7a0500105f")
    val SOME_USER_ID = UUID.fromString("23e7c082-c533-e499-63b9-6d7a05000d0e")
    val SOME_OTHER_USER_ID = UUID.fromString("aaaac082-c533-e499-63b9-6d7a05000d0f")
    val SOME_MODERATOR_ID = UUID.fromString("bbbbc082-c533-e499-63b9-6d7a05000d0f")
    const val SOME_REV = "1-93b09a4699769e6abd3bf5b2ff341e5d"
    const val SOME_NEWER_REV = "2-93b09a4699769e6abd3bf5b2ff341e5e"
    const val SOME_EVEN_NEWER_REV = "3-93b09a4699769e6abd3bf5b2ff341e5f"
    const val OWNER_STRING = "OWNER"
    const val MODERATOR_STRING = "MODERATOR"
  }

  @Mock
  private lateinit var rabbitTemplate: RabbitTemplate

  @Mock
  private lateinit var roomAccessRepository: RoomAccessRepository

  @Mock
  private lateinit var roomAccessSyncTrackerRepository: RoomAccessSyncTrackerRepository

  private lateinit var roomAccessHandler: RoomAccessHandler

  @BeforeEach
  fun setUp() {
    roomAccessHandler =
      RoomAccessHandler(rabbitTemplate, roomAccessRepository, roomAccessSyncTrackerRepository)
  }

  @Test
  fun testStartSyncOnCommand() {
    val command =
      RequestRoomAccessSyncCommand(
        SOME_ROOM_ID,
        2,
      )
    val expected =
      RoomAccessSyncRequest(
        SOME_ROOM_ID,
      )

    Mockito
      .`when`(roomAccessSyncTrackerRepository.findById(command.roomId))
      .thenReturn(
        Optional.of(
          RoomAccessSyncTracker(
            SOME_ROOM_ID,
            SOME_REV,
          ),
        ),
      )
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
    val command =
      SyncRoomAccessCommand(
        SOME_NEWER_REV,
        SOME_ROOM_ID,
        listOf(
          RoomAccessEntry(SOME_OTHER_USER_ID, OWNER_STRING),
        ),
      )
    val expectedDelete =
      RoomAccess(
        SOME_ROOM_ID,
        SOME_USER_ID,
        SOME_REV,
        OWNER_STRING,
        null,
        null,
      )
    val expectedCreate =
      RoomAccess(
        SOME_ROOM_ID,
        SOME_OTHER_USER_ID,
        SOME_NEWER_REV,
        OWNER_STRING,
        null,
        null,
      )
    val expectedTracker =
      RoomAccessSyncTracker(
        SOME_ROOM_ID,
        SOME_NEWER_REV,
      )

    Mockito
      .`when`(roomAccessSyncTrackerRepository.findById(command.roomId))
      .thenReturn(
        Optional.of(
          RoomAccessSyncTracker(
            SOME_ROOM_ID,
            SOME_REV,
          ),
        ),
      )
    Mockito
      .`when`(roomAccessSyncTrackerRepository.save(expectedTracker))
      .thenReturn(expectedTracker)
    Mockito
      .`when`(roomAccessRepository.findByRoomId(command.roomId))
      .thenReturn(
        listOf(
          // Do not delete this one
          RoomAccess(
            SOME_ROOM_ID,
            SOME_MODERATOR_ID,
            SOME_EVEN_NEWER_REV,
            MODERATOR_STRING,
            null,
            null,
          ),
          // This is old and should get deleted
          expectedDelete,
        ),
      )

    roomAccessHandler.handleSyncRoomAccessCommand(command)

    verify(roomAccessRepository, times(1)).deleteAll(listOf(expectedDelete))
    verify(roomAccessRepository, times(1)).saveAll(listOf(expectedCreate))
    verify(roomAccessSyncTrackerRepository, times(1)).save(expectedTracker)
  }

  @Test
  fun testDoNotUpdateOnOldInfo() {
    val command =
      SyncRoomAccessCommand(
        SOME_REV,
        SOME_ROOM_ID,
        listOf(
          RoomAccessEntry(SOME_USER_ID, OWNER_STRING),
        ),
      )
    val trackerCaptor = ArgumentCaptor.forClass(RoomAccessSyncTracker::class.java)

    verify(roomAccessSyncTrackerRepository, times(0)).save(trackerCaptor.capture())
  }
}
