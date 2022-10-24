package de.thm.arsnova.service.httpgateway.view

import de.thm.arsnova.service.httpgateway.exception.ForbiddenException
import de.thm.arsnova.service.httpgateway.model.Membership
import de.thm.arsnova.service.httpgateway.model.Room
import de.thm.arsnova.service.httpgateway.model.RoomAccess
import de.thm.arsnova.service.httpgateway.security.AuthProcessor
import de.thm.arsnova.service.httpgateway.service.RoomAccessService
import de.thm.arsnova.service.httpgateway.service.RoomService
import de.thm.arsnova.service.httpgateway.service.UserService
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