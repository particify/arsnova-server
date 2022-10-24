package de.thm.arsnova.service.comment.controller;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import de.thm.arsnova.service.comment.handler.CommentCommandHandler;
import de.thm.arsnova.service.comment.model.CommentStats;
import de.thm.arsnova.service.comment.model.Stats;
import de.thm.arsnova.service.comment.model.command.CalculateStats;
import de.thm.arsnova.service.comment.model.command.CalculateStatsPayload;
import de.thm.arsnova.service.comment.service.StatsService;

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
