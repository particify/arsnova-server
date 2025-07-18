package net.particify.arsnova.gateway.view

import net.particify.arsnova.gateway.model.CommentStats
import net.particify.arsnova.gateway.model.Room
import net.particify.arsnova.gateway.security.AuthProcessor
import net.particify.arsnova.gateway.service.CommentService
import net.particify.arsnova.gateway.service.ContentService
import net.particify.arsnova.gateway.service.RoomService
import net.particify.arsnova.gateway.service.WsGatewayService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class RoomViewTest(
  @Mock private val authProcessor: AuthProcessor,
  @Mock private val roomService: RoomService,
  @Mock private val contentService: ContentService,
  @Mock private val commentService: CommentService,
  @Mock private val wsGatewayService: WsGatewayService,
) {
  private val roomView = RoomView(authProcessor, roomService, contentService, commentService, wsGatewayService)

  @Test
  fun testShouldGetSummaries() {
    val roomIds: List<String> =
      listOf(
        UUID.randomUUID().toString().replace("-", ""),
        UUID.randomUUID().toString().replace("-", ""),
      )
    val userId = UUID.randomUUID().toString().replace("-", "")
    val jwtString = ""
    val testAuthentication = UsernamePasswordAuthenticationToken(userId, jwtString, listOf())
    val commentStats =
      listOf(
        CommentStats(roomIds[0], 5),
        CommentStats(roomIds[1], 10),
      )
    val contentStats =
      listOf(
        10,
        10,
      )
    val userCount =
      listOf(
        Optional.of(10),
        Optional.of(10),
      )
    val rooms =
      listOf(
        Optional.of(Room(roomIds[0], "12312312", "name for first room")),
        Optional.of(Room(roomIds[1], "11111111", "name for second room")),
      )

    given(authProcessor.getAuthentication()).willReturn(Mono.just(testAuthentication))
    given(commentService.getStats(roomIds, jwtString)).willReturn(Flux.fromIterable(commentStats))
    given(contentService.getStats(roomIds, jwtString)).willReturn(Flux.fromIterable(contentStats))
    given(wsGatewayService.getUsercount(roomIds)).willReturn(Flux.fromIterable(userCount))
    given(roomService.get(roomIds)).willReturn(Flux.fromIterable(rooms))

    StepVerifier
      .create(roomView.getSummaries(roomIds))
      .expectNextMatches { optionalRoomSummary ->
        optionalRoomSummary.isPresent
      }.expectNextMatches { optionalRoomSummary ->
        optionalRoomSummary.isPresent
      }.verifyComplete()
  }

  @Test
  fun testShouldHaveEmptyOptionalWithInvalidRoomId() {
    val roomIds: List<String> =
      listOf(
        UUID.randomUUID().toString().replace("-", ""),
        UUID.randomUUID().toString().replace("-", ""),
      )
    val userId = UUID.randomUUID().toString().replace("-", "")
    val jwtString = ""
    val testAuthentication = UsernamePasswordAuthenticationToken(userId, jwtString, listOf())
    val commentStats =
      listOf(
        CommentStats(roomIds[0], 5),
        CommentStats(roomIds[1], 10),
      )
    val contentStats =
      listOf(
        10,
        10,
      )
    val userCount =
      listOf(
        Optional.of(10),
        Optional.of(10),
      )
    val rooms =
      listOf(
        Optional.of(Room(roomIds[0], "12312312", "name for first room")),
        Optional.empty(),
      )

    given(authProcessor.getAuthentication()).willReturn(Mono.just(testAuthentication))
    given(commentService.getStats(roomIds, jwtString)).willReturn(Flux.fromIterable(commentStats))
    given(contentService.getStats(roomIds, jwtString)).willReturn(Flux.fromIterable(contentStats))
    given(wsGatewayService.getUsercount(roomIds)).willReturn(Flux.fromIterable(userCount))
    given(roomService.get(roomIds)).willReturn(Flux.fromIterable(rooms))

    StepVerifier
      .create(roomView.getSummaries(roomIds))
      .expectNextMatches { optionalRoomSummary ->
        optionalRoomSummary.isPresent
      }.expectNextMatches { optionalRoomSummary ->
        optionalRoomSummary.isEmpty
      }.verifyComplete()
  }

  @Test
  fun testShouldHaveNullUserCountWhenWsGatewayDoesntAnswer() {
    val roomIds: List<String> =
      listOf(
        UUID.randomUUID().toString().replace("-", ""),
        UUID.randomUUID().toString().replace("-", ""),
      )
    val userId = UUID.randomUUID().toString().replace("-", "")
    val jwtString = ""
    val testAuthentication = UsernamePasswordAuthenticationToken(userId, jwtString, listOf())
    val commentStats =
      listOf(
        CommentStats(roomIds[0], 5),
        CommentStats(roomIds[1], 10),
      )
    val contentStats =
      listOf(
        10,
        10,
      )
    val userCount: List<Optional<Int>> =
      listOf(
        Optional.empty(),
        Optional.empty(),
      )
    val rooms =
      listOf(
        Optional.of(Room(roomIds[0], "12312312", "name for first room")),
        Optional.of(Room(roomIds[1], "11111111", "name for second room")),
      )

    given(authProcessor.getAuthentication()).willReturn(Mono.just(testAuthentication))
    given(commentService.getStats(roomIds, jwtString)).willReturn(Flux.fromIterable(commentStats))
    given(contentService.getStats(roomIds, jwtString)).willReturn(Flux.fromIterable(contentStats))
    given(wsGatewayService.getUsercount(roomIds)).willReturn(Flux.fromIterable(userCount))
    given(roomService.get(roomIds)).willReturn(Flux.fromIterable(rooms))

    StepVerifier
      .create(roomView.getSummaries(roomIds))
      .expectNextMatches { optionalRoomSummary ->
        optionalRoomSummary
          .map { roomSummery -> roomSummery.stats.roomUserCount == null }
          .orElse(true)
      }.expectNextMatches { optionalRoomSummary ->
        optionalRoomSummary
          .map { roomSummery -> roomSummery.stats.roomUserCount == null }
          .orElse(true)
      }.verifyComplete()
  }
}
