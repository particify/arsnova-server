package de.thm.arsnova.service.httpgateway.service

import de.thm.arsnova.service.httpgateway.config.HttpGatewayProperties
import de.thm.arsnova.service.httpgateway.exception.ForbiddenException
import de.thm.arsnova.service.httpgateway.model.Announcement
import de.thm.arsnova.service.httpgateway.model.Room
import de.thm.arsnova.service.httpgateway.security.AuthProcessor
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2
import java.util.Optional

@Service
class AnnouncementService(
    private val webClient: WebClient,
    private val httpGatewayProperties: HttpGatewayProperties,
    private val authProcessor: AuthProcessor,
    private val roomAccessService: RoomAccessService,
    private val roomService: RoomService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun getByUserId(userId: String): Flux<Announcement> {
        return authProcessor.getAuthentication()
            .filter { authentication ->
                authentication.principal == userId
            }
            .switchIfEmpty(Mono.error(ForbiddenException()))
            .flatMapMany { auth ->
                val jwt: String = auth.credentials.toString()
                roomAccessService.getRoomAccessByUser(userId).map {
                    it.roomId
                }.collectList().flatMapMany { roomIds ->
                    val roomIdsStr = roomIds.joinToString(",")
                    val url = "${httpGatewayProperties.httpClient.core}/room/-/announcement/?roomIds=$roomIdsStr"
                    logger.trace("Querying core for announcements with url: {}", url)
                    webClient.get()
                        .uri(url)
                        .header("Authorization", jwt)
                        .retrieve().bodyToFlux(Announcement::class.java).cache()
                        .checkpoint("Request failed in ${this::class.simpleName}::${::getByUserId.name}.")
                }
            }
            .sort { a, b ->
                (b.updateTimestamp ?: b.creationTimestamp).compareTo(a.updateTimestamp ?: a.creationTimestamp)
            }
    }

    fun getByUserIdWithRoomName(userId: String): Flux<Announcement> {
        return getByUserId(userId).collectList().flatMapMany { announcements ->
            roomService.get(announcements.map { it.roomId }.distinct())
                .filter(Optional<Room>::isPresent)
                .map(Optional<Room>::get)
                .collectMap { it.id }
                .flatMapIterable { rooms ->
                    announcements.map {
                        it.copy(roomName = rooms[it.roomId]?.name)
                    }
                }
        }
    }
}
