package de.thm.arsnova.service.httpgateway.controller

import de.thm.arsnova.service.httpgateway.model.Stats
import de.thm.arsnova.service.httpgateway.model.SummarizedStats
import de.thm.arsnova.service.httpgateway.view.SystemView
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ResponseBody
import reactor.core.publisher.Mono

@Controller
class SystemController(
    private val systemView: SystemView
) {
    companion object {
        const val baseMapping = "/_system"
        const val serviceStatsMapping = "$baseMapping/servicestats"
        const val summarizedStatsMapping = "$baseMapping/summarizedstats"
    }

    private val logger = LoggerFactory.getLogger(this::class.java)

    @GetMapping(path = [serviceStatsMapping])
    @ResponseBody
    fun getServiceStats(): Mono<Stats> {
        logger.trace("Getting stats")
        return systemView.getServiceStats()
    }

    @GetMapping(path = [summarizedStatsMapping])
    @ResponseBody
    fun getSummarizedStats(): Mono<SummarizedStats> {
        logger.trace("Getting summarized stats")
        return systemView.getSummarizedStats()
    }
}
