package net.particify.arsnova.gateway.view

import net.particify.arsnova.gateway.exception.ForbiddenException
import net.particify.arsnova.gateway.model.Membership
import net.particify.arsnova.gateway.model.Room
import net.particify.arsnova.gateway.model.RoomAccess
import net.particify.arsnova.gateway.security.AuthProcessor
import net.particify.arsnova.gateway.service.RoomAccessService
import net.particify.arsnova.gateway.service.RoomService
import net.particify.arsnova.gateway.service.UserService
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2

@Component
class MembershipView(
  private val authProcessor: AuthProcessor,
  private val roomAccessService: RoomAccessService,
  private val roomService: RoomService,
  private val userService: UserService
) {
  fun getByUser(userId: String): Flux<Membership> {
    return authProcessor.getAuthentication()
      .filter { authentication ->
        authentication.principal == userId
      }
      .switchIfEmpty(Mono.error(ForbiddenException()))
      .map {
        roomAccessService.getRoomAccessByUser(userId)
      }
      .flatMapMany { list: Flux<RoomAccess> ->
        list
      }
      .flatMap { roomAccess: RoomAccess ->
        Flux
          .zip(
            Mono.just(roomAccess),
            roomService.get(roomAccess.roomId)
          )
          .map { (roomAccess: RoomAccess, room: Room) ->
            Membership(
              roomAccess.roomId,
              room.shortId,
              setOf(roomAccess.role),
              roomAccess.lastAccess!!
            )
          }
      }
      .groupBy { membership ->
        Membership(membership.roomId, membership.roomShortId, setOf(), membership.lastVisit)
      }
      .flatMap { groupedMemberships ->
        groupedMemberships.reduce(
          groupedMemberships.key().copy()
        ) { acc: Membership, m: Membership ->
          acc.roles = acc.roles.union(m.roles)
          if (acc.lastVisit.before(m.lastVisit)) {
            acc.lastVisit = m.lastVisit
          }
          acc
        }
      }
  }
}
