/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {
  @Bean
  @ServiceConnection
  fun postgresqlContainer(): PostgreSQLContainer<*> {
    return PostgreSQLContainer(DockerImageName.parse("postgres:18-alpine")).withUsername("arsnova")
  }
}
