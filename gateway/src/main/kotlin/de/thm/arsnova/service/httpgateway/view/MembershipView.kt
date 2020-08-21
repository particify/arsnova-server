package de.thm.arsnova.service.httpgateway.view

import de.thm.arsnova.service.httpgateway.exception.ForbiddenException
import de.thm.arsnova.service.httpgateway.model.Membership
import de.thm.arsnova.service.httpgateway.model.Room
import de.thm.arsnova.service.httpgateway.model.RoomAccess
import de.thm.arsnova.service.httpgateway.model.RoomHistoryEntry
import de.thm.arsnova.service.httpgateway.model.User
import de.thm.arsnova.service.httpgateway.security.AuthProcessor
import de.thm.arsnova.service.httpgateway.service.RoomAccessService
import de.thm.arsnova.service.httpgateway.service.RoomService
import de.thm.arsnova.service.httpgateway.service.UserService
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.Optional

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
                .flatMap { authentication ->
                    val jwt = authentication.credentials.toString()
                    userService.get(userId, jwt)
                }
                .map { user: User ->
                    user.roomHistory
                }
                .map { list: List<RoomHistoryEntry> ->
                    list.map { entry ->
                        Membership(
                            entry.roomId,
                            "shortId",
                            setOf("PARTICIPANT"),
                            entry.lastVisit
                        )
                    }
                }
                .map { list: List<Membership> ->
                    Flux
                        .zip(
                            Flux.fromIterable(list),
                            roomService.get(list.map { entry -> entry.roomId })
                        )
                        .map { t2 ->
                            t2.t2
                                    .map { room ->
                                        Mono.just(Membership(
                                                t2.t1.roomId,
                                                room.shortId,
                                                t2.t1.roles,
                                                t2.t1.lastVisit))
                                    }
                                    .orElse(Mono.empty())
                        }
                }
                .flatMapMany { list: Flux<Mono<Membership>> ->
                    list
                }
                .flatMap { list: Mono<Membership> ->
                    list
                }
                .concatWith(roomAccessService.getRoomAccessByUser(userId).flatMap { roomAccess ->
                    Flux
                        .zip(
                            Mono.just(roomAccess),
                            roomService.get(roomAccess.roomId)
                        )
                        .map { t2 ->
                            Membership(
                                t2.t1.roomId,
                                t2.t2.shortId,
                                setOf(t2.t1.role),
                                "lastVisit"
                            )
                        }
                })
                .groupBy { membership ->
                    Membership(membership.roomId, membership.roomShortId, setOf(), "")
                }
                .flatMap { groupedMemberships ->
                    groupedMemberships.reduce(groupedMemberships.key()!!.copy(), { acc: Membership, m: Membership ->
                        acc.roles = acc.roles.union(m.roles)
                        if (acc.lastVisit < m.lastVisit) {
                            acc.lastVisit = m.lastVisit
                        }
                        acc
                    })
                }
    }
}
