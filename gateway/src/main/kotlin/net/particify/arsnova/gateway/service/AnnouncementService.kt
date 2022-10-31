package net.particify.arsnova.gateway.service

import net.particify.arsnova.gateway.config.HttpGatewayProperties
import net.particify.arsnova.gateway.exception.ForbiddenException
import net.particify.arsnova.gateway.model.Announcement
import net.particify.arsnova.gateway.model.AnnouncementState
import net.particify.arsnova.gateway.model.Room
import net.particify.arsnova.gateway.model.User
import net.particify.arsnova.gateway.security.AuthProcessor
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
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

  fun getStateByUser(user: User): Mono<AnnouncementState> {
    return getByUserId(user.id).collectList().map { announcements ->
      AnnouncementState(
        announcements.count(),
        announcements
          .filter { it.creatorId != user.id }
          .count { a ->
            user.announcementReadTimestamp == null ||
              (a.updateTimestamp ?: a.creationTimestamp) > user.announcementReadTimestamp
          },
        user.announcementReadTimestamp
      )
    }
  }
}
