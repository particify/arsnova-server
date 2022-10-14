package de.thm.arsnova.service.wsgateway.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import java.time.Duration

@ConfigurationProperties
@ConstructorBinding
data class WebSocketProperties(
  val server: Server,
  val rabbitmq: Rabbitmq,
  val messagingPrefix: String,
  val stomp: Stomp,
  val security: Security,
  val httpClient: HttpClient,
  val gateway: Gateway
)

data class Server(
  val port: Int
)

data class Rabbitmq(
  val host: String,
  val port: Int,
  val username: String,
  val password: String,
  val virtualHost: String
)

data class Relay(
  val host: String,
  val port: Int,
  val user: String,
  val password: String
)

data class Stomp(
  val relay: Relay,
  val destinationPrefix: Array<String>,
  val userRegistryBroadcast: String,
  val userDestinationBroadcast: String
)

data class Security(
  val jwt: Jwt
)

data class Jwt(
  val secret: String
)

data class HttpClient(
  val authService: String
)

data class Gateway(
  val eventRateLimit: EventRateLimit
)

data class EventRateLimit(
  val threshold: Long,
  val duration: Duration,
  val tokensPerTimeframe: Long,
  val burstCapacity: Long
)
