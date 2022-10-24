package de.thm.arsnova.service.authservice.management

import de.thm.arsnova.service.authservice.persistence.RoomAccessRepository
import io.micrometer.core.instrument.DistributionSummary
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalUnit
import java.util.Date

@Service
class MetricsService(
  meterRegistry: MeterRegistry,
  private val roomAccessRepository: RoomAccessRepository
) {
  companion object {
    const val GATHERING_INTERVAL_DAILY = 10 * 60 * 1000L
    const val GATHERING_INTERVAL_WEEKLY = 60 * 60 * 1000L
    const val GATHERING_INTERVAL_MONTHLY = 24 * 60 * 60 * 1000L
    const val ROLE_TYPE_TAG_NAME = "role.type"
  }

  val roomMembershipsDailyDistribution = DistributionSummary
    .builder("arsnova.room.memberships.daily")
    .publishPercentileHistogram()
    .maximumExpectedValue(1000.0)
    .register(meterRegistry)
  val roomMembershipsWeeklyDistribution = DistributionSummary
    .builder("arsnova.room.memberships.weekly")
    .publishPercentileHistogram()
    .maximumExpectedValue(1000.0)
    .register(meterRegistry)
  val roomMembershipsMonthlyDistribution = DistributionSummary
    .builder("arsnova.room.memberships.monthly")
    .publishPercentileHistogram()
    .maximumExpectedValue(1000.0)
    .register(meterRegistry)

  var dailyUserCount = 0L
  var dailyOwnerCount = 0L
  var weeklyUserCount = 0L
  var weeklyOwnerCount = 0L
  var monthlyUserCount = 0L
  var monthlyOwnerCount = 0L

  init {
    Gauge
      .builder("arsnova.users.active.daily") { this.dailyOwnerCount }
      .tag(ROLE_TYPE_TAG_NAME, "owner")
      .register(meterRegistry)
    Gauge
      .builder("arsnova.users.active.daily") { this.dailyUserCount - this.dailyOwnerCount }
      .tag(ROLE_TYPE_TAG_NAME, "non-owner")
      .register(meterRegistry)
    Gauge
      .builder("arsnova.users.active.weekly") { this.weeklyOwnerCount }
      .tag(ROLE_TYPE_TAG_NAME, "owner")
      .register(meterRegistry)
    Gauge
      .builder("arsnova.users.active.weekly") { this.weeklyUserCount - this.weeklyOwnerCount }
      .tag(ROLE_TYPE_TAG_NAME, "non-owner")
      .register(meterRegistry)
    Gauge
      .builder("arsnova.users.active.monthly") { this.monthlyOwnerCount }
      .tag(ROLE_TYPE_TAG_NAME, "owner")
      .register(meterRegistry)
    Gauge
      .builder("arsnova.users.active.monthly") { this.monthlyUserCount - this.monthlyOwnerCount }
      .tag(ROLE_TYPE_TAG_NAME, "non-owner")
      .register(meterRegistry)
  }

  @Scheduled(fixedRate = GATHERING_INTERVAL_DAILY)
  fun updateRoomMembershipsDailyMetrics() {
    updateUserCountMetrics(1, ChronoUnit.DAYS)
    updateRoomMembershipsMetrics(1, ChronoUnit.DAYS, roomMembershipsDailyDistribution)
  }

  @Scheduled(fixedRate = GATHERING_INTERVAL_WEEKLY)
  fun updateRoomMembershipsWeeklyMetrics() {
    updateUserCountMetrics(1, ChronoUnit.WEEKS)
    updateRoomMembershipsMetrics(1, ChronoUnit.WEEKS, roomMembershipsWeeklyDistribution)
  }

  @Scheduled(fixedRate = GATHERING_INTERVAL_MONTHLY)
  fun updateRoomMembershipsMonthlyMetrics() {
    updateUserCountMetrics(1, ChronoUnit.MONTHS)
    updateRoomMembershipsMetrics(1, ChronoUnit.MONTHS, roomMembershipsMonthlyDistribution)
  }

  private fun updateUserCountMetrics(interval: Long, unit: TemporalUnit) {
    val userCount = roomAccessRepository.countDistinctUserIdByLastAccessAfter(
      Date.from(LocalDateTime.now().minus(interval, unit).toInstant(ZoneOffset.UTC))
    )
    val ownerCount = roomAccessRepository.countDistinctUserIdByRoleAndLastAccessAfter(
      "CREATOR",
      Date.from(LocalDateTime.now().minus(interval, unit).toInstant(ZoneOffset.UTC))
    )
    when (unit) {
      ChronoUnit.DAYS -> {
        this.dailyUserCount = userCount
        this.dailyOwnerCount = ownerCount
      }
      ChronoUnit.WEEKS -> {
        this.weeklyUserCount = userCount
        this.weeklyOwnerCount = ownerCount
      }
      ChronoUnit.MONTHS -> {
        this.monthlyUserCount = userCount
        this.monthlyOwnerCount = ownerCount
      }
      else -> throw IllegalArgumentException("Invalid value for unit.")
    }
  }

  private fun updateRoomMembershipsMetrics(interval: Long, unit: TemporalUnit, distributionSummary: DistributionSummary) {
    val membershipCounts = roomAccessRepository.countByLastAccessAfterAndGroupByRoomId(
      Date.from(LocalDateTime.now().minus(interval, unit).toInstant(ZoneOffset.UTC))
    )
    for (count in membershipCounts) {
      distributionSummary.record(count.toDouble())
    }
  }
}
