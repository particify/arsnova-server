package net.particify.arsnova.gateway.controller

import net.particify.arsnova.gateway.model.RoomSummary
import net.particify.arsnova.gateway.view.RoomView
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import reactor.core.publisher.Flux
import java.util.Optional

@Controller
class RoomController(
  private val roomView: RoomView,
) {
  companion object {
    const val BASE_MAPPING = "/_view/room"
    const val SUMMARY_MAPPING = "$BASE_MAPPING/summary"
  }

  private val logger = LoggerFactory.getLogger(javaClass)

  @GetMapping(path = [SUMMARY_MAPPING])
  @ResponseBody
  fun getRoomSummaries(
    @RequestParam ids: List<String>,
  ): Flux<Optional<RoomSummary>> {
    logger.trace("Getting room summaries by ids: {}", ids)
    return roomView.getSummaries(ids)
  }
}
