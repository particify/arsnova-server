package net.particify.arsnova.gateway.filter

import io.github.bucket4j.BandwidthBuilder
import io.github.bucket4j.Bucket
import io.github.bucket4j.ConsumptionProbe
import net.particify.arsnova.gateway.config.HttpGatewayProperties
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.cloud.gateway.filter.ratelimit.AbstractRateLimiter
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter
import org.springframework.cloud.gateway.support.ConfigurationService
import org.springframework.security.web.util.matcher.IpAddressMatcher
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

@Component
class RequestRateLimiter(
  private val httpGatewayProperties: HttpGatewayProperties,
  private val configurationService: ConfigurationService,
) : AbstractRateLimiter<RequestRateLimiter.Config>(Config::class.java, CONFIGURATION_PROPERTY_NAME, configurationService) {
  companion object {
    private const val CONFIGURATION_PROPERTY_NAME = "rate-limiter"
    private const val QUERY_BUCKET_PREFIX = "QUERY_"
    private const val COMMAND_BUCKET_PREFIX = "COMMAND_"
    private val queryMethods = arrayOf("GET", "OPTIONS")
  }

  private val logger: Logger = LoggerFactory.getLogger(this::class.java)
  private val defaultConfig = Config(httpGatewayProperties)
  private val ipBucketMap: MutableMap<String, Bucket> = ConcurrentHashMap()
  private val allowedIpAddresses =
    httpGatewayProperties
      .gateway.rateLimit.whitelistedIps
      .map { IpAddressMatcher(it) }

  /*
  The id is the key extracted from the KeyResolver configured in GatewayConfig.
  Format: <HTTPMethod>,<IP>
  Example: GET,172.18.0.18
   */
  override fun isAllowed(
    routeId: String,
    id: String,
  ): Mono<RateLimiter.Response> {
    logger.trace("Checking rate limit for {} from {}.", routeId, id)

    var routeConfig: Config? = config[routeId]
    if (routeConfig == null) {
      routeConfig = defaultConfig
    }

    val httpMethod = id.split(",")[0]
    val ipAddr = id.split(",")[1]
    val isQuery = queryMethods.any { it == httpMethod }

    if (allowedIpAddresses.any { it.matches(ipAddr) }) {
      return Mono.just(RateLimiter.Response(true, mapOf("RateLimit-Remaining" to "infinite")))
    }

    val bucket: Bucket =
      if (isQuery) {
        ipBucketMap.computeIfAbsent(QUERY_BUCKET_PREFIX + ipAddr) { _: String ->
          val bandwidth =
            BandwidthBuilder
              .builder()
              .capacity(routeConfig.queryBurstCapacity)
              .refillIntervally(routeConfig.queryTokensPerTimeframe, routeConfig.duration)
              .build()
          Bucket.builder().addLimit(bandwidth).build()
        }
      } else {
        ipBucketMap.computeIfAbsent(COMMAND_BUCKET_PREFIX + ipAddr) { _: String ->
          val bandwidth =
            BandwidthBuilder
              .builder()
              .capacity(routeConfig.commandBurstCapacity)
              .refillIntervally(routeConfig.commandTokensPerTimeframe, routeConfig.duration)
              .build()
          Bucket.builder().addLimit(bandwidth).build()
        }
      }

    if (bucket.availableTokens in 1..10) {
      // Check early so logs don't get spammed
      logger.info("Rate limit nearly exceeded for {} by {}.", routeId, id)
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
    private val httpGatewayProperties: HttpGatewayProperties,
  ) {
    val queryTokensPerTimeframe: Long = httpGatewayProperties.gateway.rateLimit.queryTokensPerTimeframe
    val queryBurstCapacity: Long = httpGatewayProperties.gateway.rateLimit.queryBurstCapacity
    val commandTokensPerTimeframe: Long = httpGatewayProperties.gateway.rateLimit.commandTokensPerTimeframe
    val commandBurstCapacity: Long = httpGatewayProperties.gateway.rateLimit.commandBurstCapacity

    // the time window
    val duration: Duration = httpGatewayProperties.gateway.rateLimit.duration
  }
}
