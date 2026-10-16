/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.room

import jakarta.persistence.EntityManager
import java.util.Locale
import java.util.UUID
import net.particify.arsnova.core4.TestcontainersConfiguration
import net.particify.arsnova.core4.room.internal.RoomUserAliasServiceImpl
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

private val ROOM_ID = UUID.fromString("3f9cecb5-6dbf-45cc-b630-4af700d474b6")
private val OWNER_ID = UUID.fromString("9c778494-9b52-424d-8c7f-f4936629facb")
private val PARTICIPANT_ID = UUID.fromString("43389844-708b-469d-8d22-abdd2eb88777")
private val ENGLISH = Locale.of("en")

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration::class)
@Transactional
class RoomUserAliasPersistenceTests {
  @Autowired lateinit var roomUserAliasService: RoomUserAliasServiceImpl
  @Autowired lateinit var entityManager: EntityManager

  @Test
  fun shouldDeleteAllAliasesOfARoom() {
    roomUserAliasService.set(ROOM_ID, OWNER_ID, "Captain", null, ENGLISH)
    roomUserAliasService.set(ROOM_ID, PARTICIPANT_ID, null, 2, ENGLISH)
    entityManager.flush()
    Assertions.assertEquals(2, roomUserAliasService.deleteAllByRoomId(ROOM_ID))
    entityManager.clear()
    Assertions.assertTrue(roomUserAliasService.findByRoomId(ROOM_ID).isEmpty())
  }

  @Test
  fun shouldDeleteOnlyTheAliasesOfAUser() {
    roomUserAliasService.set(ROOM_ID, OWNER_ID, "Captain", null, ENGLISH)
    roomUserAliasService.set(ROOM_ID, PARTICIPANT_ID, null, 2, ENGLISH)
    entityManager.flush()
    Assertions.assertEquals(1, roomUserAliasService.deleteAllByUserId(PARTICIPANT_ID))
    entityManager.clear()
    Assertions.assertEquals(
        listOf(OWNER_ID), roomUserAliasService.findByRoomId(ROOM_ID).map { it.user!!.id })
  }
}
