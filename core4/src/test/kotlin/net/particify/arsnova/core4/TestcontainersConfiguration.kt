/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4

import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

/**
 * A plain connection details bean rather than @ServiceConnection, whose bean registration does not
 * survive AOT processing, or dynamic properties, which are applied only after an embedded web
 * server has already created the DataSource for its filters.
 */
@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {
  @Bean
  fun postgresqlContainer(): PostgreSQLContainer {
    return PostgreSQLContainer(DockerImageName.parse("postgres:18-alpine")).withUsername("arsnova")
  }

  @Bean
  fun postgresqlConnectionDetails(container: PostgreSQLContainer): JdbcConnectionDetails =
      object : JdbcConnectionDetails {
        override fun getJdbcUrl(): String = container.jdbcUrl

        override fun getUsername(): String = container.username

        override fun getPassword(): String = container.password
      }
}
