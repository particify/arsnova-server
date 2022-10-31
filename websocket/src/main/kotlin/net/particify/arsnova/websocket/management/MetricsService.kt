package net.particify.arsnova.websocket.management

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.DistributionSummary
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.binder.BaseUnits
import net.particify.arsnova.websocket.event.RoomUserCountChangedEvent
import net.particify.arsnova.websocket.service.RoomSubscriptionService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.web.socket.messaging.SessionConnectEvent
import org.springframework.web.socket.messaging.SessionDisconnectEvent
import org.springframework.web.socket.messaging.SessionSubscribeEvent
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

@Service
class MetricsService(
  meterRegistry: MeterRegistry,
  private val roomSubscriptionService: RoomSubscriptionService,
) {
  companion object {
    const val GATHERING_INTERVAL = 60 * 1000L
    const val MIN_USERS = 5
    const val ACTIVE_ROOM_MIN_USERS = 8
    const val ACTIVE_ROOM_MIN_USERS_TOLERANCE_FACTOR = 2.0 / 3
    const val SESSION_END_USER_FACTOR = 1.0 / 3
    const val SESSION_RESTART_USER_FACTOR = 1.5
  }

  val logger: Logger = LoggerFactory.getLogger(this::class.java)
  val connectCounter = Counter
    .builder("websocket.broker.connect")
    .baseUnit(BaseUnits.EVENTS)
    .register(meterRegistry)
  val disconnectCounter = Counter
    .builder("websocket.broker.disconnect")
    .baseUnit(BaseUnits.EVENTS)
    .register(meterRegistry)
  val subscribeCounter = Counter
    .builder("websocket.broker.subscribe")
    .baseUnit(BaseUnits.EVENTS)
    .register(meterRegistry)
  val unsubscribeCounter = Counter
    .builder("websocket.broker.unsubscribe")
    .baseUnit(BaseUnits.EVENTS)
    .register(meterRegistry)
  val roomUserDistribution = DistributionSummary
    .builder("arsnova.room.users")
    .publishPercentileHistogram()
    .maximumExpectedValue(1000.0)
    .register(meterRegistry)
  val roomSessionUserDistribution = DistributionSummary
    .builder("arsnova.room.session.users")
    .publishPercentileHistogram()
    .maximumExpectedValue(1000.0)
    .register(meterRegistry)
  val roomSessionDurationDistribution = DistributionSummary
    .builder("arsnova.room.session.duration")
    .baseUnit("minutes")
    .publishPercentileHistogram()
    .maximumExpectedValue(360.0)
    .register(meterRegistry)
  val activeRooms = ConcurrentHashMap<String, ActiveRoomMetrics>()

  @Scheduled(fixedRate = GATHERING_INTERVAL)
  fun updateRoomUsersMetrics() {
    for (count in roomSubscriptionService.getUserCounts()) {
      roomUserDistribution.record(count.toDouble())
    }
  }

  @EventListener
  fun handleConnectEvent(sessionConnectEvent: SessionConnectEvent) {
    connectCounter.increment()
  }

  @EventListener
  fun handleDisconnectEvent(sessionDisconnectEvent: SessionDisconnectEvent) {
    disconnectCounter.increment()
  }

  @EventListener
  fun handleSubscribeEvent(sessionSubscribeEvent: SessionSubscribeEvent) {
    subscribeCounter.increment()
  }

  @EventListener
  fun handleCUnsubscribeEvent(sessionUnsubscribeEvent: SessionUnsubscribeEvent) {
    unsubscribeCounter.increment()
  }

  @EventListener
  fun handleRoomUserCountChangedEvent(event: RoomUserCountChangedEvent) {
    if (event.count < MIN_USERS) {
      return
    }

    synchronized(activeRooms) {
      val metrics = activeRooms[event.roomId]
      when {
        metrics == null -> {
          // Session just started or has a low user count
          if (event.count < ACTIVE_ROOM_MIN_USERS) {
            return
          }
          activeRooms[event.roomId] = ActiveRoomMetrics(event.count, LocalDateTime.now())
        }
        event.count > metrics.maxUserCount -> {
          // User count in session is rising
          activeRooms[event.roomId] = metrics.copy(maxUserCount = event.count)
        }
        event.count < ACTIVE_ROOM_MIN_USERS * ACTIVE_ROOM_MIN_USERS_TOLERANCE_FACTOR -> {
          // User count dropped below minimum -> session is ending
          activeRooms.remove(event.roomId)
          if (metrics.decliningMinUserCount == -1) {
            trackSessionEnd(metrics)
          }
        }
        event.count < metrics.maxUserCount * SESSION_END_USER_FACTOR -> {
          // User count dropped significantly (compared to maximum)
          when {
            metrics.decliningMinUserCount == -1 -> {
              // Session is ending
              activeRooms[event.roomId] = metrics.copy(decliningMinUserCount = event.count)
              trackSessionEnd(metrics)
            }
            event.count >= ACTIVE_ROOM_MIN_USERS &&
              event.count > metrics.decliningMinUserCount * SESSION_RESTART_USER_FACTOR -> {
              // User count increases after session has ended -> reset/new session
              activeRooms[event.roomId] = ActiveRoomMetrics(event.count, LocalDateTime.now())
            }
            event.count < metrics.decliningMinUserCount -> {
              // User count further declines after session has ended
              activeRooms[event.roomId] = metrics.copy(decliningMinUserCount = event.count)
            }
          }
        }
      }
    }
  }

  private fun trackSessionEnd(metrics: ActiveRoomMetrics) {
    roomSessionUserDistribution.record(metrics.maxUserCount.toDouble())
    roomSessionDurationDistribution.record(Duration.between(metrics.sessionStart, LocalDateTime.now()).toMinutes().toDouble())
  }

  data class ActiveRoomMetrics(
    val maxUserCount: Int,
    val sessionStart: LocalDateTime,
    val decliningMinUserCount: Int = -1,
  )
}
