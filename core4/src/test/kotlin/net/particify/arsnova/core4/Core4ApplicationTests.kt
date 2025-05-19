/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4

import net.particify.arsnova.core4.room.internal.RoomRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import

@SpringBootTest
@Import(TestcontainersConfiguration::class)
class Core4ApplicationTests {
  @Suppress("EmptyFunctionBlock") @Test fun contextLoads() {}

  @Autowired lateinit var roomRepository: RoomRepository
}
