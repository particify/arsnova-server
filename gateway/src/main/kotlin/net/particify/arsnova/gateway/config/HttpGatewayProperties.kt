package net.particify.arsnova.gateway.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties
data class HttpGatewayProperties(
  val security: Security,
  val httpClient: HttpClient,
  val routing: Routing,
  val gateway: Gateway
)

data class Security(
  val jwt: Jwt
)

data class Jwt(
  val publicSecret: String,
  val internalSecret: String,
  val serverId: String,
  val validityPeriod: Duration
)

data class HttpClient(
  val authService: String,
  val commentService: String,
  val core: String,
  val wsGateway: String,
  val subscriptionService: String?
)

data class Routing(
  val endpoints: Endpoints
)

data class Endpoints(
  val core: String,
  val wsGateway: String,
  val commentService: String,
  val roomaccessService: String,
  val importService: String?,
  val formattingService: String,
  val attachmentService: String?,
  val subscriptionService: String?,
  val proxyMetrics: String?
)

data class Gateway(
  val rateLimit: RateLimit,
  val requireMembership: Boolean
)

data class RateLimit(
  val duration: Duration,
  val queryTokensPerTimeframe: Long,
  val queryBurstCapacity: Long,
  val commandTokensPerTimeframe: Long,
  val commandBurstCapacity: Long,
  val whitelistedIps: List<String>
)
