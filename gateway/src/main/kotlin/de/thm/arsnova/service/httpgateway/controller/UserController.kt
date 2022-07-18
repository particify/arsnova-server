package de.thm.arsnova.service.httpgateway.controller

import de.thm.arsnova.service.httpgateway.exception.ForbiddenException
import de.thm.arsnova.service.httpgateway.model.Announcement
import de.thm.arsnova.service.httpgateway.model.AnnouncementState
import de.thm.arsnova.service.httpgateway.security.AuthProcessor
import de.thm.arsnova.service.httpgateway.service.AnnouncementService
import de.thm.arsnova.service.httpgateway.service.UserService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.ResponseBody
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Controller
class UserController(
    private val announcementService: AnnouncementService,
    private val userService: UserService,
    private val authProcessor: AuthProcessor
) {
    companion object {
        const val baseMapping = "/user/{userId}"
        const val announcementMapping = "$baseMapping/announcement"
        const val announcementStateMapping = "$announcementMapping/state"
    }

    private val logger = LoggerFactory.getLogger(this::class.java)

    @GetMapping(path = [announcementMapping])
    @ResponseBody
    fun getAnnouncements(@PathVariable userId: String): Flux<Announcement> {
        logger.trace("Getting announcements")
        return announcementService.getByUserIdWithRoomName(userId)
    }

    @PostMapping(path = [announcementMapping])
    @ResponseBody
    fun postAnnouncements(@PathVariable userId: String): Flux<Announcement> {
        logger.trace("Getting announcements and updating timestamp")
        return authProcessor.getAuthentication()
            .filter { authentication ->
                authentication.principal == userId
            }
            .switchIfEmpty(Mono.error(ForbiddenException()))
            .map { it.credentials.toString() }
            .flatMapMany { jwt ->
                announcementService.getByUserIdWithRoomName(userId).doOnComplete {
                    userService.updateAnnouncementReadTimestamp(userId, jwt).subscribe()
                }
            }
    }

    @GetMapping(path = [announcementStateMapping])
    @ResponseBody
    fun getAnnouncementState(@PathVariable userId: String): Mono<AnnouncementState> {
        logger.trace("Getting announcement state")
        return authProcessor.getAuthentication()
            .filter { authentication ->
                authentication.principal == userId
            }
            .switchIfEmpty(Mono.error(ForbiddenException()))
            .map { it.credentials.toString() }
            .flatMap { jwt ->
                userService.get(userId, jwt).flatMap {
                    announcementService.getStateByUser(it)
                }
            }
    }
}
