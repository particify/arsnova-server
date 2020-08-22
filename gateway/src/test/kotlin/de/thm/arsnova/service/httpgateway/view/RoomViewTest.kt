package de.thm.arsnova.service.httpgateway.view

import de.thm.arsnova.service.httpgateway.model.CommentStats
import de.thm.arsnova.service.httpgateway.model.Room
import de.thm.arsnova.service.httpgateway.model.RoomSummary
import de.thm.arsnova.service.httpgateway.security.AuthProcessor
import de.thm.arsnova.service.httpgateway.service.CommentService
import de.thm.arsnova.service.httpgateway.service.ContentService
import de.thm.arsnova.service.httpgateway.service.RoomService
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
class RoomViewTest(
        @Mock private val authProcessor: AuthProcessor,
        @Mock private val roomService: RoomService,
        @Mock private val contentService: ContentService,
        @Mock private val commentService: CommentService
) {
    private val roomView = RoomView(authProcessor, roomService, contentService, commentService)

    @Test
    fun testShouldGetSummaries() {
        val roomIds: List<String> = listOf(
                UUID.randomUUID().toString().replace("-", ""),
                UUID.randomUUID().toString().replace("-", "")
        )
        val userId = UUID.randomUUID().toString().replace("-", "")
        val jwtString = ""
        val testAuthentication = UsernamePasswordAuthenticationToken(userId, jwtString, listOf())
        val commentStats = listOf(
                CommentStats(roomIds[0], 5),
                CommentStats(roomIds[1], 10)
        )
        val contentStats = listOf(
                10,
                10
        )
        val rooms = listOf(
                Optional.of(Room(roomIds[0], "12312312", "name for first room")),
                Optional.of(Room(roomIds[1], "11111111", "name for second room"))
        )

        given(authProcessor.getAuthentication()).willReturn(Mono.just(testAuthentication))
        given(commentService.getStats(roomIds, jwtString)).willReturn(Flux.fromIterable(commentStats))
        given(contentService.getStats(roomIds, jwtString)).willReturn(Flux.fromIterable(contentStats))
        given(roomService.get(roomIds)).willReturn(Flux.fromIterable(rooms))

        roomView.getSummaries(roomIds)
                .onErrorResume {
                    // There should not be an error
                    assert(false)
                    Flux.empty()
                }
                .collectList()
                .subscribe { optionalRoomSummaries: List<Optional<RoomSummary>> ->
                    assert(optionalRoomSummaries.size == roomIds.size)
                    optionalRoomSummaries.map { optionalRoomSummary ->
                        assert(optionalRoomSummary.isPresent)
                    }
                }
    }

    @Test
    fun testShouldHaveEmptyOptionalWithInvalidRoomId() {
        val roomIds: List<String> = listOf(
                UUID.randomUUID().toString().replace("-", ""),
                UUID.randomUUID().toString().replace("-", "")
        )
        val userId = UUID.randomUUID().toString().replace("-", "")
        val jwtString = ""
        val testAuthentication = UsernamePasswordAuthenticationToken(userId, jwtString, listOf())
        val commentStats = listOf(
                CommentStats(roomIds[0], 5),
                CommentStats(roomIds[1], 10)
        )
        val contentStats = listOf(
                10,
                10
        )
        val rooms = listOf(
                Optional.of(Room(roomIds[0], "12312312", "name for first room")),
                Optional.empty()
        )

        given(authProcessor.getAuthentication()).willReturn(Mono.just(testAuthentication))
        given(commentService.getStats(roomIds, jwtString)).willReturn(Flux.fromIterable(commentStats))
        given(contentService.getStats(roomIds, jwtString)).willReturn(Flux.fromIterable(contentStats))
        given(roomService.get(roomIds)).willReturn(Flux.fromIterable(rooms))

        roomView.getSummaries(roomIds)
                .onErrorResume {
                    // There should not be an error
                    assert(false)
                    Flux.empty()
                }
                .collectList()
                .subscribe { optionalRoomSummaries: List<Optional<RoomSummary>> ->
                    assert(optionalRoomSummaries.size == roomIds.size)
                    assert(optionalRoomSummaries.get(0).isPresent)
                    assert(optionalRoomSummaries.get(1).isEmpty)
                }
    }
}
