package de.thm.arsnova.service.httpgateway.controller

import de.thm.arsnova.service.httpgateway.model.RoomSummary
import de.thm.arsnova.service.httpgateway.view.RoomView
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import reactor.core.publisher.Flux
import java.util.Optional

@Controller
class RoomController(
    private val roomView: RoomView
) {

    companion object {
        const val baseMapping = "/_view/room"
        const val summaryMapping = "$baseMapping/summary"
    }

    private val logger = LoggerFactory.getLogger(javaClass)

    @GetMapping(path = [summaryMapping])
    @ResponseBody
    fun getRoomSummaries(
        @RequestParam ids: List<String>
    ) : Flux<Optional<RoomSummary>> {
        logger.trace("Getting room summaries by ids: {}", ids)
        return roomView.getSummaries(ids)
    }
}
