package de.thm.arsnova.service.httpgateway.view

import de.thm.arsnova.service.httpgateway.exception.ForbiddenException
import de.thm.arsnova.service.httpgateway.model.Room
import de.thm.arsnova.service.httpgateway.model.RoomAccess
import de.thm.arsnova.service.httpgateway.model.RoomHistoryEntry
import de.thm.arsnova.service.httpgateway.model.User
import de.thm.arsnova.service.httpgateway.security.AuthProcessor
import de.thm.arsnova.service.httpgateway.service.RoomAccessService
import de.thm.arsnova.service.httpgateway.service.RoomService
import de.thm.arsnova.service.httpgateway.service.UserService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.Optional
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
        val visitedRoomIds: List<String> = listOf(
                UUID.randomUUID().toString().replace("-", ""),
                UUID.randomUUID().toString().replace("-", "")
        )
        val ownedRoomIds: List<String> = listOf(
                UUID.randomUUID().toString().replace("-", ""),
                UUID.randomUUID().toString().replace("-", "")
        )
        val jwtString = ""
        val testAuthentication = UsernamePasswordAuthenticationToken(userId, jwtString, listOf())
        val userRoomHistories = listOf(
                RoomHistoryEntry(visitedRoomIds[0], "last visit"),
                RoomHistoryEntry(visitedRoomIds[1], "last visit")
        )
        val testUser = User(userId, userRoomHistories)
        val visitedRooms = listOf(
                Optional.of(Room(visitedRoomIds[0], "12312312", "name for first visited room")),
                Optional.of(Room(visitedRoomIds[1], "11112222", "name for second visited room"))
        )
        val userRoomAccess = listOf(
                RoomAccess(ownedRoomIds[0], userId, "rev", "OWNER", null),
                RoomAccess(ownedRoomIds[1], userId, "rev", "OWNER", null)
        )
        val ownedRooms = listOf(
                Room(ownedRoomIds[0], "22222222", "name for first owned room"),
                Room(ownedRoomIds[1], "22223333", "name for second owned room")
        )

        given(authProcessor.getAuthentication()).willReturn(Mono.just(testAuthentication))
        given(userService.get(userId, jwtString)).willReturn(Mono.just(testUser))
        given(roomService.get(visitedRoomIds)).willReturn(Flux.fromIterable(visitedRooms))
        given(roomAccessService.getRoomAccessByUser(userId)).willReturn(Flux.fromIterable(userRoomAccess))
        given(roomService.get(ownedRoomIds[0])).willReturn(Mono.just(ownedRooms[0]))
        given(roomService.get(ownedRoomIds[1])).willReturn(Mono.just(ownedRooms[1]))

        membershipView.getByUser(userId)
                .onErrorResume {
                    // There should not be an error
                    assert(false)
                    Flux.empty()
                }
                .collectList()
                .subscribe { membershipList ->
                    assert(membershipList.size == visitedRoomIds.size + ownedRoomIds.size )
                }
    }

    @Test
    fun testShouldForbidForAnotherUserId() {
        val userId = UUID.randomUUID().toString().replace("-", "")
        val otherUserId = UUID.randomUUID().toString().replace("-", "")
        val jwtString = ""
        val testAuthentication = UsernamePasswordAuthenticationToken(userId, jwtString, listOf())

        given(authProcessor.getAuthentication()).willReturn(Mono.just(testAuthentication))
        given(roomAccessService.getRoomAccessByUser(otherUserId)).willReturn(Flux.empty())

        membershipView.getByUser(otherUserId)
                .onErrorResume { error ->
                    assert(error is ForbiddenException)
                    Flux.empty()
                }
                .subscribe()
    }

    @Test
    fun testShouldHandleUserWithOnlyVisitedRooms() {
        val userId = UUID.randomUUID().toString().replace("-", "")
        val visitedRoomIds: List<String> = listOf(
                UUID.randomUUID().toString().replace("-", ""),
                UUID.randomUUID().toString().replace("-", "")
        )
        val jwtString = ""
        val testAuthentication = UsernamePasswordAuthenticationToken(userId, jwtString, listOf())
        val userRoomHistories = listOf(
                RoomHistoryEntry(visitedRoomIds[0], "last visit"),
                RoomHistoryEntry(visitedRoomIds[1], "last visit")
        )
        val testUser = User(userId, userRoomHistories)
        val visitedRooms = listOf(
                Optional.of(Room(visitedRoomIds[0], "12312312", "name for first visited room")),
                Optional.of(Room(visitedRoomIds[1], "11112222", "name for second visited room"))
        )

        given(authProcessor.getAuthentication()).willReturn(Mono.just(testAuthentication))
        given(userService.get(userId, jwtString)).willReturn(Mono.just(testUser))
        given(roomService.get(visitedRoomIds)).willReturn(Flux.fromIterable(visitedRooms))
        given(roomAccessService.getRoomAccessByUser(userId)).willReturn(Flux.empty())

        membershipView.getByUser(userId)
                .onErrorResume {
                    // There should not be an error
                    assert(false)
                    Flux.empty()
                }
                .collectList()
                .subscribe { membershipList ->
                    assert(membershipList.size == visitedRoomIds.size)
                }
    }

    @Test
    fun testShouldHandleUserWithOnlyOwnedRooms() {
        val userId = UUID.randomUUID().toString().replace("-", "")
        val ownedRoomIds: List<String> = listOf(
                UUID.randomUUID().toString().replace("-", ""),
                UUID.randomUUID().toString().replace("-", "")
        )
        val jwtString = ""
        val testAuthentication = UsernamePasswordAuthenticationToken(userId, jwtString, listOf())
        val testUser = User(userId, listOf())
        val userRoomAccess = listOf(
                RoomAccess(ownedRoomIds[0], userId, "rev", "OWNER", null),
                RoomAccess(ownedRoomIds[1], userId, "rev", "OWNER", null)
        )
        val ownedRooms = listOf(
                Room(ownedRoomIds[0], "22222222", "name for first owned room"),
                Room(ownedRoomIds[1], "22223333", "name for second owned room")
        )

        given(authProcessor.getAuthentication()).willReturn(Mono.just(testAuthentication))
        given(userService.get(userId, jwtString)).willReturn(Mono.just(testUser))
        given(roomAccessService.getRoomAccessByUser(userId)).willReturn(Flux.fromIterable(userRoomAccess))
        given(roomService.get(ownedRoomIds[0])).willReturn(Mono.just(ownedRooms[0]))
        given(roomService.get(ownedRoomIds[1])).willReturn(Mono.just(ownedRooms[1]))

        membershipView.getByUser(userId)
                .onErrorResume {
                    // There should not be an error
                    assert(false)
                    Flux.empty()
                }
                .collectList()
                .subscribe { membershipList ->
                    assert(membershipList.size == ownedRoomIds.size)
                }
    }

    @Test
    fun testShouldHandleStaleRoomInUserRoomAccess() {
        val userId = UUID.randomUUID().toString().replace("-", "")
        val jwtString = ""
        val testAuthentication = UsernamePasswordAuthenticationToken(userId, jwtString, listOf())
        val testUser = User(userId, listOf())
        val ownedRoomIds: List<String> = listOf(
                UUID.randomUUID().toString().replace("-", ""),
                UUID.randomUUID().toString().replace("-", "")
        )
        val userRoomAccess = listOf(
                RoomAccess(ownedRoomIds[0], userId, "rev", "OWNER", null),
                RoomAccess(ownedRoomIds[1], userId, "rev", "OWNER", null)
        )
        val notAStaleRoom = Room(ownedRoomIds[0], "22222222", "name for first owned room")

        given(authProcessor.getAuthentication()).willReturn(Mono.just(testAuthentication))
        given(userService.get(userId, jwtString)).willReturn(Mono.just(testUser))
        given(roomAccessService.getRoomAccessByUser(userId)).willReturn(Flux.fromIterable(userRoomAccess))
        given(roomService.get(ownedRoomIds[0])).willReturn(Mono.just(notAStaleRoom))
        given(roomService.get(ownedRoomIds[1])).willReturn(Mono.empty())

        membershipView.getByUser(userId)
                .onErrorResume {
                    // There should not be an error
                    Flux.empty()
                }
                .collectList()
                .switchIfEmpty(Mono.just(listOf()))
                .subscribe { membershipList ->
                    assert(membershipList.size == ownedRoomIds.size - 1)
                }
    }
}
