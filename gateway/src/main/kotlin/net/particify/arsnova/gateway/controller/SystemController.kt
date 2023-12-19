package net.particify.arsnova.gateway.controller

import net.particify.arsnova.gateway.model.Stats
import net.particify.arsnova.gateway.model.SummarizedStats
import net.particify.arsnova.gateway.view.SystemView
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Controller
import org.springframework.util.MultiValueMap
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import reactor.core.publisher.Mono

@Controller
class SystemController(
  private val systemView: SystemView,
) {
  companion object {
    const val BASE_MAPPING = "/_system"
    const val SERVICE_STATS_MAPPING = "$BASE_MAPPING/servicestats"
    const val SUMMARIZED_STATS_MAPPING = "$BASE_MAPPING/summarizedstats"
  }

  private val logger = LoggerFactory.getLogger(this::class.java)

  @GetMapping(path = [SERVICE_STATS_MAPPING])
  @ResponseBody
  fun getServiceStats(
    @RequestParam params: MultiValueMap<String, String>,
  ): Mono<Stats> {
    logger.trace("Getting stats")
    return systemView.getServiceStats(params)
  }

  @GetMapping(path = [SUMMARIZED_STATS_MAPPING])
  @ResponseBody
  fun getSummarizedStats(
    @RequestParam params: MultiValueMap<String, String>,
  ): Mono<SummarizedStats> {
    logger.trace("Getting summarized stats")
    return systemView.getSummarizedStats(params)
  }
}
