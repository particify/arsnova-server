package net.particify.arsnova.gateway.view

import net.particify.arsnova.gateway.exception.ForbiddenException
import net.particify.arsnova.gateway.model.Room
import net.particify.arsnova.gateway.model.RoomAccess
import net.particify.arsnova.gateway.security.AuthProcessor
import net.particify.arsnova.gateway.service.RoomAccessService
import net.particify.arsnova.gateway.service.RoomService
import net.particify.arsnova.gateway.service.UserService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.util.Date
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class MembershipViewTest(
  @Mock private val authProcessor: AuthProcessor,
  @Mock private val roomAccessService: RoomAccessService,
  @Mock private val roomService: RoomService,
  @Mock private val userService: UserService
) {
  private val membershipView = MembershipView(authProcessor, roomAccessService, roomService, userService)

  @Test
  fun testShouldGetByUser() {
    val userId = UUID.randomUUID().toString().replace("-", "")
    val testAuthentication = UsernamePasswordAuthenticationToken(userId, "jwtString", listOf())
    val ownedRoomId = UUID.randomUUID().toString().replace("-", "")
    val visitedRoomId = UUID.randomUUID().toString().replace("-", "")
    val roomAccessList = listOf(
      RoomAccess(ownedRoomId, "22222222", "0-0", "CREATOR", Date()),
      RoomAccess(visitedRoomId, "22223333", "0-0", "PARTICIPANT", Date())
    )
    val ownedRoom = Room(ownedRoomId, "22222222", "A name")
    val visitedRoom = Room(visitedRoomId, "22223333", "A second room")

    given(authProcessor.getAuthentication()).willReturn(Mono.just(testAuthentication))
    given(roomAccessService.getRoomAccessByUser(userId)).willReturn(Flux.fromIterable(roomAccessList))
    given(roomService.get(ownedRoomId)).willReturn(Mono.just(ownedRoom))
    given(roomService.get(visitedRoomId)).willReturn(Mono.just(visitedRoom))

    StepVerifier
      .create(membershipView.getByUser(userId))
      .expectNextCount(2)
      .verifyComplete()
  }

  @Test
  fun testShouldForbidForAnotherUserId() {
    val userId = UUID.randomUUID().toString().replace("-", "")
    val jwtUserId = UUID.randomUUID().toString().replace("-", "")
    val otherUserId = UUID.randomUUID().toString().replace("-", "")
    val testAuthentication = UsernamePasswordAuthenticationToken(jwtUserId, "jwtString", listOf())

    given(authProcessor.getAuthentication()).willReturn(Mono.just(testAuthentication))
    given(roomAccessService.getRoomAccessByUser(otherUserId)).willReturn(Flux.empty())

    StepVerifier
      .create(membershipView.getByUser(userId))
      .expectErrorMatches { e ->
        e is ForbiddenException
      }
      .verify()
  }
}
