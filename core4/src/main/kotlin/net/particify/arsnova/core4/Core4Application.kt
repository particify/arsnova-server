/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

@SpringBootApplication @EnableJpaAuditing @ConfigurationPropertiesScan class Core4Application

@Suppress("SpreadOperator")
fun main(args: Array<String>) {
  runApplication<Core4Application>(*args)
}
