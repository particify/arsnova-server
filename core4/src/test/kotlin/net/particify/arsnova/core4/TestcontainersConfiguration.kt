/* Copyright 2025-2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.test.context.DynamicPropertyRegistrar
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

/**
 * Properties rather than @ServiceConnection: the connection details beans the latter registers do
 * not survive AOT processing, so test contexts would not load in AOT mode.
 */
@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {
  @Bean
  fun postgresqlContainer(): PostgreSQLContainer {
    return PostgreSQLContainer(DockerImageName.parse("postgres:18-alpine")).withUsername("arsnova")
  }

  @Bean
  fun postgresqlProperties(container: PostgreSQLContainer) = DynamicPropertyRegistrar {
    it.add("spring.datasource.url", container::getJdbcUrl)
    it.add("spring.datasource.username", container::getUsername)
    it.add("spring.datasource.password", container::getPassword)
  }
}
