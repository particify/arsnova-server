/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.room

import java.util.UUID
import net.particify.arsnova.core4.TestcontainersConfiguration
import net.particify.arsnova.core4.room.internal.MembershipServiceImpl
import net.particify.arsnova.core4.user.User
import net.particify.arsnova.core4.user.UserService
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

/** Account from the `dev` Liquibase fixtures which owns [ROOM_ID]. */
private val OWNER_USER_ID = UUID.fromString("43389844-708b-469d-8d22-abdd2eb88777")
private val ROOM_ID = UUID.fromString("659f7444-d5e3-41bc-b1c7-c1e3b085cd56")

/** Account creation publishes an event, which permission evaluation does not depend on. */
private const val EXTERNALIZATION_PROPERTY = "spring.modulith.events.externalization.enabled=false"

private const val ADMINISTER = "administer"
private const val DELETE = "delete"
private const val MODERATE = "moderate"
private const val READ = "read"
private const val WRITE = "write"

private val ALL_PERMISSIONS = setOf(ADMINISTER, DELETE, MODERATE, READ, WRITE)

@SpringBootTest(properties = [EXTERNALIZATION_PROPERTY])
@ActiveProfiles("test")
@Import(TestcontainersConfiguration::class)
class RoomPermissionEvaluatorTests {
  @Autowired lateinit var roomPermissionEvaluator: RoomPermissionEvaluator
  @Autowired lateinit var membershipService: MembershipServiceImpl
  @Autowired lateinit var userService: UserService

  @Test
  fun shouldGrantEveryPermissionToOwner() {
    assertPermissions(User(id = OWNER_USER_ID), ALL_PERMISSIONS)
  }

  /** The owner's branch does not enumerate permissions, so permissions added later apply to it. */
  @Test
  fun shouldGrantUnknownPermissionToOwner() {
    Assertions.assertEquals(true, hasPermission(User(id = OWNER_USER_ID), "unknown-permission"))
  }

  @Test
  fun shouldGrantModerateReadAndWriteToEditor() {
    assertPermissions(member(RoomRole.EDITOR), setOf(MODERATE, READ, WRITE))
  }

  @Test
  fun shouldGrantModerateAndReadToModerator() {
    assertPermissions(member(RoomRole.MODERATOR), setOf(MODERATE, READ))
  }

  @Test
  fun shouldGrantReadToParticipant() {
    assertPermissions(member(RoomRole.PARTICIPANT), setOf(READ))
  }

  @Test
  fun shouldDenyEveryPermissionWithoutMembership() {
    assertPermissions(userService.createAccount(), setOf())
  }

  /** No branch other than the owner's covers them, which is what restricts them to the owner. */
  @Test
  fun shouldRestrictDeleteAndAdministerToOwner() {
    val owner = User(id = OWNER_USER_ID)
    listOf(DELETE, ADMINISTER).forEach { permission ->
      Assertions.assertEquals(true, hasPermission(owner, permission), permission)
      listOf(RoomRole.EDITOR, RoomRole.MODERATOR, RoomRole.PARTICIPANT).forEach { role ->
        Assertions.assertEquals(false, hasPermission(member(role), permission), "$role $permission")
      }
    }
  }

  private fun assertPermissions(user: User, granted: Set<String>) {
    ALL_PERMISSIONS.forEach { permission ->
      Assertions.assertEquals(
          granted.contains(permission), hasPermission(user, permission), permission)
    }
  }

  private fun hasPermission(user: User, permission: String): Boolean? =
      roomPermissionEvaluator
          .hasPermission(user, roomPermissionEvaluator.findOneByKey(ROOM_ID), permission)
          .hasPermission

  private fun member(role: RoomRole): User {
    val user = userService.createAccount()
    membershipService.save(
        Membership(room = Room(id = ROOM_ID), user = User(id = user.id), role = role))
    return user
  }
}
