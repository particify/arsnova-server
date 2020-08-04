package de.thm.arsnova.service.httpgateway.view

import de.thm.arsnova.service.httpgateway.model.Membership
import de.thm.arsnova.service.httpgateway.model.RoomAccess
import de.thm.arsnova.service.httpgateway.model.RoomHistoryEntry
import de.thm.arsnova.service.httpgateway.model.User
import de.thm.arsnova.service.httpgateway.service.RoomAccessService
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
                .map { list: List<RoomHistoryEntry>? ->
                    list?.map { entry ->
                        Membership(
                                entry.roomId,
                                "shortId",
                                listOf("GUEST"),
                                entry.lastVisit
                        )
                    }
                }
                .flatMapMany { list ->
                    Flux.fromIterable(list!!)
                }
                .concatWith(roomAccessService.getRoomAccessByUser(userId).map { roomAccess ->
                    Membership(
                            roomAccess.roomId!!,
                            "shortId",
                            listOf(roomAccess.role!!),
                            "lastVisit"
                    )
                })
    }
}
