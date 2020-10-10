package de.thm.arsnova.service.comment.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.thm.arsnova.service.comment.model.Stats;
import de.thm.arsnova.service.comment.service.StatsService;

@RestController("StatsController")
@RequestMapping(StatsController.REQUEST_MAPPING)
public class StatsController extends AbstractEntityController {
    private static final Logger logger = LoggerFactory.getLogger(StatsController.class);

    protected static final String REQUEST_MAPPING = "/stats";
    private static final String STATS_GET_MAPPNIG = "";

    private final StatsService service;

    @Autowired
    public StatsController(
            StatsService service
    ) {
        this.service = service;
    }

    @GetMapping(STATS_GET_MAPPNIG)
    public Stats get() {
        return service.get();
    }
}
