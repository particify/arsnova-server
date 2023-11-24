package net.particify.arsnova.authz.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties
data class AuthServiceProperties(
  val server: Server,
  val rabbitmq: Rabbitmq,
  val spring: Spring,
  val security: Security,
)

data class Server(
  val port: Int,
)

data class Rabbitmq(
  val host: String,
  val port: Int,
  val username: String,
  val password: String,
  val virtualHost: String,
)

data class Spring(
  val datasource: Datasource,
  val jpa: Jpa,
)

data class Datasource(
  val url: String,
  val driverClassName: String,
  val platform: String,
  val username: String,
  val password: String,
)

data class Jpa(
  val hibernate: Hibernate,
)

data class Hibernate(
  val ddlAuto: String,
)

data class Security(
  val jwt: Jwt,
  val authorizeUriHeader: String,
  val authorizeUriPrefix: String,
)

data class Jwt(
  val idpIssuer: String?,
  val secret: String,
  val serverId: String,
  val validityPeriod: Duration,
)
