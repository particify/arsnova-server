/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.room

import java.util.Locale
import java.util.UUID
import net.particify.arsnova.core4.TestcontainersConfiguration
import net.particify.arsnova.core4.room.internal.api.CreateRoomInput
import net.particify.arsnova.core4.room.internal.api.DuplicateRoomInput
import net.particify.arsnova.core4.room.internal.api.RoomMutationController
import net.particify.arsnova.core4.system.security.JwtUtils
import net.particify.arsnova.core4.system.security.UserJwtAuthentication
import net.particify.arsnova.core4.system.security.UserJwtAuthenticationProvider
import net.particify.arsnova.core4.user.User
import net.particify.arsnova.core4.user.UserService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate

/** Account from the `dev` Liquibase fixtures which owns [OWNED_ROOM_ID]. */
private val VERIFIED_USER_ID = UUID.fromString("43389844-708b-469d-8d22-abdd2eb88777")
private val OWNED_ROOM_ID = UUID.fromString("659f7444-d5e3-41bc-b1c7-c1e3b085cd56")

/** Short ID of the English demo room from the `dev` Liquibase fixtures. */
private const val DEMO_SHORT_ID = 11111111

private const val DEMO_LANGUAGE_PROPERTY = "room.demo[0].language=en"
private const val DEMO_SHORT_ID_PROPERTY = "room.demo[0].short-id=$DEMO_SHORT_ID"
private const val AUTO_ASSIGN_TO_PROPERTY = "security.room-creator-role.auto-assign-to"

/** Room events reach RabbitMQ, which authorization does not depend on. */
private const val EXTERNALIZATION_PROPERTY = "spring.modulith.events.externalization.enabled=false"

/**
 * Drives the mutations through the method security proxy with an authentication built the way
 * requests build it, so the authority [UserJwtAuthenticationProvider] derives is what the
 * annotations are evaluated against.
 */
abstract class RoomCreationAuthorizationTestSupport {
  @Autowired lateinit var roomMutationController: RoomMutationController
  @Autowired lateinit var userService: UserService
  @Autowired lateinit var jwtUtils: JwtUtils
  @Autowired lateinit var authenticationProvider: UserJwtAuthenticationProvider
  @Autowired lateinit var transactionManager: PlatformTransactionManager

  @AfterEach
  fun clearSecurityContext() {
    SecurityContextHolder.clearContext()
  }

  protected fun authenticateVerifiedUser(): User = authenticate(VERIFIED_USER_ID)

  protected fun authenticateUnverifiedUser(): User =
      authenticate(checkNotNull(userService.createAccount().id))

  protected fun createRoom(user: User): Room =
      roomMutationController.createRoom(CreateRoomInput("Created room"), user)

  protected fun duplicateOwnedRoom(user: User): Room =
      roomMutationController.duplicateRoom(DuplicateRoomInput(OWNED_ROOM_ID, "Copy"), user)

  protected fun duplicateDemoRoom(user: User): Room =
      roomMutationController.duplicateDemoRoom(user, Locale.ENGLISH)

  /**
   * The provider reads the account's roles, which are only reachable while a transaction is open:
   * outside a request there is no session to load them lazily from.
   */
  private fun authenticate(userId: UUID): User {
    val token = jwtUtils.encodeJwt(userId.toString(), listOf())
    val authentication =
        checkNotNull(
            TransactionTemplate(transactionManager).execute {
              authenticationProvider.authenticate(UserJwtAuthentication(token))
            })
    SecurityContextHolder.getContext().authentication = authentication
    return authentication.principal as User
  }
}

@SpringBootTest(
    properties =
        [
            DEMO_LANGUAGE_PROPERTY,
            DEMO_SHORT_ID_PROPERTY,
            EXTERNALIZATION_PROPERTY,
            "$AUTO_ASSIGN_TO_PROPERTY=all-accounts"])
@ActiveProfiles("test")
@Import(TestcontainersConfiguration::class)
class RoomCreationForAllAccountsTests : RoomCreationAuthorizationTestSupport() {
  @Test
  fun shouldAllowCreateRoomForUnverifiedAccount() {
    Assertions.assertNotNull(createRoom(authenticateUnverifiedUser()))
  }

  @Test
  fun shouldAllowDuplicateDemoRoomForUnverifiedAccount() {
    Assertions.assertNotNull(duplicateDemoRoom(authenticateUnverifiedUser()))
  }

  @Test
  fun shouldAllowDuplicateRoomForVerifiedOwner() {
    Assertions.assertNotNull(duplicateOwnedRoom(authenticateVerifiedUser()))
  }
}

@SpringBootTest(
    properties =
        [
            DEMO_LANGUAGE_PROPERTY,
            DEMO_SHORT_ID_PROPERTY,
            EXTERNALIZATION_PROPERTY,
            "$AUTO_ASSIGN_TO_PROPERTY=verified-accounts"])
@ActiveProfiles("test")
@Import(TestcontainersConfiguration::class)
class RoomCreationForVerifiedAccountsTests : RoomCreationAuthorizationTestSupport() {
  @Test
  fun shouldDenyCreateRoomForUnverifiedAccount() {
    val user = authenticateUnverifiedUser()
    assertThrows<AccessDeniedException> { createRoom(user) }
  }

  @Test
  fun shouldDenyDuplicateDemoRoomForUnverifiedAccount() {
    val user = authenticateUnverifiedUser()
    assertThrows<AccessDeniedException> { duplicateDemoRoom(user) }
  }

  @Test
  fun shouldAllowCreateRoomForVerifiedAccount() {
    Assertions.assertNotNull(createRoom(authenticateVerifiedUser()))
  }

  @Test
  fun shouldAllowDuplicateRoomForVerifiedOwner() {
    Assertions.assertNotNull(duplicateOwnedRoom(authenticateVerifiedUser()))
  }
}

@SpringBootTest(
    properties =
        [
            DEMO_LANGUAGE_PROPERTY,
            DEMO_SHORT_ID_PROPERTY,
            EXTERNALIZATION_PROPERTY,
            "$AUTO_ASSIGN_TO_PROPERTY=none"])
@ActiveProfiles("test")
@Import(TestcontainersConfiguration::class)
class RoomCreationForNoAccountsTests : RoomCreationAuthorizationTestSupport() {
  @Test
  fun shouldDenyCreateRoomForVerifiedAccount() {
    val user = authenticateVerifiedUser()
    assertThrows<AccessDeniedException> { createRoom(user) }
  }

  @Test
  fun shouldDenyDuplicateDemoRoomForVerifiedAccount() {
    val user = authenticateVerifiedUser()
    assertThrows<AccessDeniedException> { duplicateDemoRoom(user) }
  }

  /** The room permission on its own no longer suffices. */
  @Test
  fun shouldDenyDuplicateRoomForVerifiedOwner() {
    val user = authenticateVerifiedUser()
    assertThrows<AccessDeniedException> { duplicateOwnedRoom(user) }
  }
}
