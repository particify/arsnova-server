package de.thm.arsnova.service.authservice.management

import de.thm.arsnova.service.authservice.persistence.RoomAccessRepository
import io.micrometer.core.instrument.DistributionSummary
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.Logger
import org.slf4j.LoggerFactory
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

    @Scheduled(fixedRate = GATHERING_INTERVAL_DAILY)
    fun updateRoomMembershipsDailyMetrics() {
        updateRoomMembershipsMetrics(1, ChronoUnit.DAYS, roomMembershipsDailyDistribution)
    }

    @Scheduled(fixedRate = GATHERING_INTERVAL_WEEKLY)
    fun updateRoomMembershipsWeeklyMetrics() {
        updateRoomMembershipsMetrics(1, ChronoUnit.WEEKS, roomMembershipsWeeklyDistribution)
    }

    @Scheduled(fixedRate = GATHERING_INTERVAL_MONTHLY)
    fun updateRoomMembershipsMonthlyMetrics() {
        updateRoomMembershipsMetrics(1, ChronoUnit.MONTHS, roomMembershipsMonthlyDistribution)
    }

    private fun updateRoomMembershipsMetrics(interval: Long, unit: TemporalUnit, distributionSummary: DistributionSummary) {
        val membershipCounts = roomAccessRepository.countByLastAccessAfterAndGroupByRoomId(
            Date.from(LocalDateTime.now().minus(interval, unit).toInstant(ZoneOffset.UTC)))
        for (count in membershipCounts) {
            distributionSummary.record(count.toDouble())
        }
    }
}
