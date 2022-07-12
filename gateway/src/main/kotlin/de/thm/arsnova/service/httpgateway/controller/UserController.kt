package de.thm.arsnova.service.httpgateway.controller

import de.thm.arsnova.service.httpgateway.model.Announcement
import de.thm.arsnova.service.httpgateway.service.AnnouncementService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.ResponseBody
import reactor.core.publisher.Flux

@Controller
class UserController(
    private val announcementService: AnnouncementService
) {
    companion object {
        const val baseMapping = "/user/{userId}"
        const val announcementMapping = "$baseMapping/announcement"
    }

    private val logger = LoggerFactory.getLogger(this::class.java)

    @GetMapping(path = [announcementMapping])
    @ResponseBody
    fun getAnnouncements(@PathVariable userId: String): Flux<Announcement> {
        logger.trace("Getting announcements")
        return announcementService.getByUserId(userId)
    }
}
