package net.particify.arsnova.websocket.management

import com.ninjasquad.springmockk.MockkBean
import com.ninjasquad.springmockk.SpykBean
import io.mockk.verify
import net.particify.arsnova.websocket.event.RoomUserCountChangedEvent
import net.particify.arsnova.websocket.service.RoomSubscriptionService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationEventPublisher
import java.time.LocalDateTime
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt

@SpringBootTest
class MetricsServiceTest {
  @MockkBean
  private lateinit var roomSubscriptionService: RoomSubscriptionService

  @SpykBean
  private lateinit var metricsService: MetricsService

  @Autowired
  private lateinit var applicationEventPublisher: ApplicationEventPublisher

  @Test
  fun testSessionWithUserCountBelowThresholdShouldNotBeTracked() {
    testHandleRoomUserCountChangedEvent(MetricsService.ACTIVE_ROOM_MIN_USERS - 1)
  }

  @Test
  fun testSessionWithUserCountAtThresholdShouldBeTracked() {
    testHandleRoomUserCountChangedEvent(MetricsService.ACTIVE_ROOM_MIN_USERS)
  }

  @Test
  fun testSessionWithUserCountOverThresholdShouldBeTracked() {
    testHandleRoomUserCountChangedEvent(MetricsService.ACTIVE_ROOM_MIN_USERS + 1)
  }

  private fun testHandleRoomUserCountChangedEvent(maxUserCount: Int) {
    val roomId = "Room-With-$maxUserCount-Users"
    for (i in MetricsService.ACTIVE_ROOM_MIN_USERS..maxUserCount) {
      val after = LocalDateTime.now().minusSeconds(1)
      applicationEventPublisher.publishEvent(
        RoomUserCountChangedEvent(
          roomId = roomId,
          count = i
        )
      )
      val before = LocalDateTime.now().plusSeconds(1)
      if (i < MetricsService.ACTIVE_ROOM_MIN_USERS) {
        assertNull(
          metricsService.activeRooms[roomId],
          "Room should not have metrics."
        )
        continue
      }
      assertEquals(
        i, metricsService.activeRooms[roomId]?.maxUserCount,
        "'maxUserCount' does not have the expected value."
      )
      assertNotNull(
        metricsService.activeRooms[roomId]?.sessionStart,
        "'sessionStart' is expected to be not null."
      )
      assertTrue(
        metricsService.activeRooms[roomId]?.sessionStart?.isAfter(after)!!,
        "'sessionStart' is not in expected time range."
      )
      assertTrue(
        metricsService.activeRooms[roomId]?.sessionStart?.isBefore(before)!!,
        "'sessionStart' is not in expected time range."
      )
      assertEquals(
        -1, metricsService.activeRooms[roomId]?.decliningMinUserCount,
        "'decliningMinUserCount' does not have the expected value."
      )
    }
    verify(exactly = 0) { metricsService["trackSessionEnd"](any<MetricsService.ActiveRoomMetrics>()) }
    if (maxUserCount < MetricsService.ACTIVE_ROOM_MIN_USERS) {
      return
    }

    for (
      i in maxUserCount downTo max(
        floor(maxUserCount * MetricsService.SESSION_END_USER_FACTOR),
        floor(MetricsService.ACTIVE_ROOM_MIN_USERS * MetricsService.ACTIVE_ROOM_MIN_USERS_TOLERANCE_FACTOR)
      ).roundToInt()
    ) {
      applicationEventPublisher.publishEvent(
        RoomUserCountChangedEvent(
          roomId = roomId,
          count = i
        )
      )
      if (i < maxUserCount * MetricsService.SESSION_END_USER_FACTOR ||
        i < MetricsService.ACTIVE_ROOM_MIN_USERS * MetricsService.ACTIVE_ROOM_MIN_USERS_TOLERANCE_FACTOR
      ) {
        verify(exactly = 1) { metricsService["trackSessionEnd"](any<MetricsService.ActiveRoomMetrics>()) }
      } else {
        assertEquals(
          maxUserCount, metricsService.activeRooms[roomId]?.maxUserCount,
          "'maxUserCount' does not have the expected value."
        )
        assertEquals(
          -1, metricsService.activeRooms[roomId]?.decliningMinUserCount,
          "'decliningMinUserCount' does not have the expected value."
        )
      }
    }
  }
}
