package net.particify.arsnova.authz.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties
data class AuthServiceProperties(
  val server: Server,
  val rabbitmq: Rabbitmq,
  val spring: Spring
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

data class Spring(
  val datasource: Datasource,
  val jpa: Jpa
)

data class Datasource(
  val url: String,
  val driverClassName: String,
  val platform: String,
  val username: String,
  val password: String
)

data class Jpa(
  val hibernate: Hibernate
)

data class Hibernate(
  val ddlAuto: String
)
