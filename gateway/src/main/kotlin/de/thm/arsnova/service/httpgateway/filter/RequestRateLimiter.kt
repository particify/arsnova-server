package de.thm.arsnova.service.httpgateway.filter

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import io.github.bucket4j.Bucket4j
import io.github.bucket4j.ConsumptionProbe
import io.github.bucket4j.Refill
import org.springframework.cloud.gateway.filter.ratelimit.AbstractRateLimiter
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter
import org.springframework.stereotype.Component
import org.springframework.validation.Validator
import reactor.core.publisher.Mono
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

@Component
class RequestRateLimiter(
        validator: Validator
) : AbstractRateLimiter<RequestRateLimiter.Config>(Config::class.java, CONFIGURATION_PROPERTY_NAME, validator) {
    private val defaultConfig = Config()
    private val ipBucketMap: MutableMap<String, Bucket> = ConcurrentHashMap()

    override fun isAllowed(routeId: String, id: String): Mono<RateLimiter.Response> {
        var routeConfig: Config? = config[routeId]
        if (routeConfig == null) {
            routeConfig = defaultConfig
        }

        val bucket: Bucket = ipBucketMap.computeIfAbsent(id) { k: String ->
            val refill: Refill = Refill.intervally(routeConfig.tokensPerTimeframe, routeConfig.duration)
            val limit: Bandwidth = Bandwidth.classic(routeConfig.burstCapacity, refill)
            Bucket4j.builder().addLimit(limit).build()
        }

        // tryConsume returns false immediately if no tokens available with the bucket
        val probe: ConsumptionProbe = bucket.tryConsumeAndReturnRemaining(1)
        return if (probe.isConsumed) {
            // the limit is not exceeded
            Mono.just(RateLimiter.Response(true, probe.remainingTokens))
        } else {
            // limit is exceeded
            Mono.just(RateLimiter.Response(false, -1))
        }
    }

    companion object {
        private const val CONFIGURATION_PROPERTY_NAME = "rate-limiter"
    }

    class Config {
        val tokensPerTimeframe: Long = 10
        val burstCapacity: Long = 10
        // the time window
        val duration: Duration = Duration.ofSeconds(5)
    }
}
