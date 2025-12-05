package net.particify.arsnova.core.controller;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import net.particify.arsnova.core.model.RoomStatistics;
import net.particify.arsnova.core.service.RoomStatisticsService;

@RestController
@EntityRequestMapping(RoomController.REQUEST_MAPPING)
public class RoomStatisticsController {
  protected static final String STATS_MAPPING = RoomController.DEFAULT_ID_MAPPING + "/stats";

  private RoomStatisticsService roomStatisticsService;

  public RoomStatisticsController(
      @Qualifier("securedRoomStatisticsService") final RoomStatisticsService roomStatisticsService) {
    this.roomStatisticsService = roomStatisticsService;
  }

  @GetMapping(STATS_MAPPING)
  public RoomStatistics getStats(
      @PathVariable final String id,
      @RequestParam(required = false) final String view) {
    final RoomStatistics roomStatistics = "read-extended".equals(view)
        ? roomStatisticsService.getAllRoomStatistics(id)
        : roomStatisticsService.getPublicRoomStatistics(id);

    return roomStatistics;
  }
}
