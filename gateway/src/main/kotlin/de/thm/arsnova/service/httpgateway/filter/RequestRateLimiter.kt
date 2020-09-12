package de.thm.arsnova.service.httpgateway.filter

import de.thm.arsnova.service.httpgateway.config.HttpGatewayProperties
import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import io.github.bucket4j.Bucket4j
import io.github.bucket4j.ConsumptionProbe
import io.github.bucket4j.Refill
import org.springframework.cloud.gateway.filter.ratelimit.AbstractRateLimiter
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter
import org.springframework.cloud.gateway.support.ConfigurationService
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

@Component
class RequestRateLimiter(
        private val httpGatewayProperties: HttpGatewayProperties,
        private val configurationService: ConfigurationService
) : AbstractRateLimiter<RequestRateLimiter.Config>(Config::class.java, CONFIGURATION_PROPERTY_NAME, configurationService) {

    companion object {
        private const val CONFIGURATION_PROPERTY_NAME = "rate-limiter"
        private const val QUERY_BUCKET_PREFIX = "QUERY_"
        private const val COMMAND_BUCKET_PREFIX = "COMMAND_"
        private val queryMethods = arrayOf("GET", "OPTIONS")
    }

    private val defaultConfig = Config(httpGatewayProperties)
    private val ipBucketMap: MutableMap<String, Bucket> = ConcurrentHashMap()

    /*
    The id is the key extracted from the KeyResolver configured in GatewayConfig.
    Format: <HTTPMethod>,<IP>
    Example: GET,172.18.0.18
     */
    override fun isAllowed(routeId: String, id: String): Mono<RateLimiter.Response> {
        var routeConfig: Config? = config[routeId]
        if (routeConfig == null) {
            routeConfig = defaultConfig
        }

        val httpMethod = id.split(",")[0]
        val ipAddr = id.split(",")[1]
        val isQuery = queryMethods.any { it == httpMethod }

        val bucket: Bucket = if (isQuery) {
            ipBucketMap.computeIfAbsent(QUERY_BUCKET_PREFIX + ipAddr) { _: String ->
                val refill: Refill = Refill.intervally(routeConfig.queryTokensPerTimeframe, routeConfig.duration)
                val limit: Bandwidth = Bandwidth.classic(routeConfig.queryBurstCapacity, refill)
                Bucket4j.builder().addLimit(limit).build()
            }
        } else {
            ipBucketMap.computeIfAbsent(COMMAND_BUCKET_PREFIX + ipAddr) { _: String ->
                val refill: Refill = Refill.intervally(routeConfig.commandTokensPerTimeframe, routeConfig.duration)
                val limit: Bandwidth = Bandwidth.classic(routeConfig.commandBurstCapacity, refill)
                Bucket4j.builder().addLimit(limit).build()
            }
        }

        // tryConsume returns false immediately if no tokens available with the bucket
        val probe: ConsumptionProbe = bucket.tryConsumeAndReturnRemaining(1)
        return if (probe.isConsumed) {
            // the limit is not exceeded
            Mono.just(RateLimiter.Response(true, mapOf("RateLimit-Remaining" to probe.remainingTokens.toString())))
        } else {
            // limit is exceeded
            Mono.just(RateLimiter.Response(false, mapOf("RateLimit-Remaining" to "none")))
        }
    }

    class Config(
            private val httpGatewayProperties: HttpGatewayProperties
    ) {
        val queryTokensPerTimeframe: Long = httpGatewayProperties.gateway.rateLimit.queryTokensPerTimeframe
        val queryBurstCapacity: Long = httpGatewayProperties.gateway.rateLimit.queryBurstCapacity
        val commandTokensPerTimeframe: Long = httpGatewayProperties.gateway.rateLimit.commandTokensPerTimeframe
        val commandBurstCapacity: Long = httpGatewayProperties.gateway.rateLimit.commandBurstCapacity
        // the time window
        val duration: Duration = httpGatewayProperties.gateway.rateLimit.duration
    }
}
