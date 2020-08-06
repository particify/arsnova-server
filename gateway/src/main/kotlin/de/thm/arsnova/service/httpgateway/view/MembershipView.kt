package de.thm.arsnova.service.httpgateway.view

import de.thm.arsnova.service.httpgateway.model.Membership
import de.thm.arsnova.service.httpgateway.model.Room
import de.thm.arsnova.service.httpgateway.model.RoomAccess
import de.thm.arsnova.service.httpgateway.model.RoomHistoryEntry
import de.thm.arsnova.service.httpgateway.model.User
import de.thm.arsnova.service.httpgateway.service.RoomAccessService
import de.thm.arsnova.service.httpgateway.service.RoomService
import de.thm.arsnova.service.httpgateway.service.UserService
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Component
class MembershipView(
    private val roomAccessService: RoomAccessService,
    private val roomService: RoomService,
    private val userService: UserService
) {
    fun getByUser(userId: String): Flux<Membership> {
        return ReactiveSecurityContextHolder.getContext()
                .map { securityContext ->
                    securityContext.authentication.principal
                }
                .cast(String::class.java)
                .flatMap { jwt ->
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
                            listOf("GUEST"),
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
                            Membership(
                                t2.t1.roomId,
                                t2.t2.shortId,
                                t2.t1.roles,
                                t2.t1.lastVisit
                            )
                        }
                }
                .flatMapMany { list: Flux<Membership> ->
                    list
                }
                .concatWith(roomAccessService.getRoomAccessByUser(userId).flatMap { roomAccess ->
                    Flux
                        .zip(
                            Mono.just(roomAccess),
                            roomService.get(roomAccess.roomId!!)
                        )
                        .map { t2 ->
                            Membership(
                                t2.t1.roomId!!,
                                t2.t2.shortId,
                                listOf(t2.t1.role!!),
                                "lastVisit"
                            )
                        }
                })
    }
}
