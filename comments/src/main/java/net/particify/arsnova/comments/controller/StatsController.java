package net.particify.arsnova.comments.controller;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import net.particify.arsnova.comments.handler.CommentCommandHandler;
import net.particify.arsnova.comments.model.CommentStats;
import net.particify.arsnova.comments.model.Stats;
import net.particify.arsnova.comments.model.command.CalculateStats;
import net.particify.arsnova.comments.model.command.CalculateStatsPayload;
import net.particify.arsnova.comments.service.StatsService;

@RestController("StatsController")
@RequestMapping(StatsController.REQUEST_MAPPING)
public class StatsController extends AbstractEntityController {
  private static final Logger logger = LoggerFactory.getLogger(StatsController.class);

  protected static final String REQUEST_MAPPING = "/stats";
  private static final String COMMENT_STATS_BY_ROOMS_MAPPING = "/comment-stats-by-rooms";
  private static final String STATS_GET_MAPPNIG = "";

  private final StatsService service;
  private final CommentCommandHandler commentCommandHandler;

  @Autowired
  public StatsController(
      StatsService service,
      CommentCommandHandler commentCommandHandler
  ) {
    this.service = service;
    this.commentCommandHandler = commentCommandHandler;
  }

  @GetMapping(STATS_GET_MAPPNIG)
  public Stats get() {
    return service.get();
  }

  @GetMapping(COMMENT_STATS_BY_ROOMS_MAPPING)
  public List<CommentStats> statsByRoom(
      @RequestParam final List<String> roomIds
  ) {
    CalculateStatsPayload p = new CalculateStatsPayload(roomIds);
    CalculateStats command = new CalculateStats(p);

    return commentCommandHandler.handle(command);
  }
}
