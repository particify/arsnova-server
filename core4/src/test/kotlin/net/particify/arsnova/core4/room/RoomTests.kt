/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.room

import java.util.UUID
import net.particify.arsnova.core4.TestcontainersConfiguration
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import

@SpringBootTest
@Import(TestcontainersConfiguration::class)
class RoomTests {
  @Autowired lateinit var membershipService: MembershipService

  @Test
  fun shouldFindCorrectMembership() {
    val roomId = UUID.fromString("3f9cecb5-6dbf-45cc-b630-4af700d474b6")
    val userId = UUID.fromString("9c778494-9b52-424d-8c7f-f4936629facb")
    val membership = membershipService.findOneByRoomIdAndUserId(roomId, userId)
    Assertions.assertNotNull(membership)
    Assertions.assertEquals(roomId, membership!!.room!!.id)
    Assertions.assertEquals(userId, membership!!.user!!.id)
  }
}
