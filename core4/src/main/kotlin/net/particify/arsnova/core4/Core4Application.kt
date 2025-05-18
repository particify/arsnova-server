/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication class Core4Application

@Suppress("SpreadOperator")
fun main(args: Array<String>) {
  runApplication<Core4Application>(*args)
}
