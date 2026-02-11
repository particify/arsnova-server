package net.particify.arsnova.core.controller;

import static net.particify.arsnova.core.controller.RoomDataGenerationController.REQUEST_MAPPING;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import net.particify.arsnova.core.service.DataGenerationService;

@RestController
@EntityRequestMapping(REQUEST_MAPPING)
public class RoomDataGenerationController {
  protected static final String REQUEST_MAPPING = "/room" + RoomController.DEFAULT_ID_MAPPING + "/generate-random-data";
  private final DataGenerationService dataGenerationService;

  public RoomDataGenerationController(
      @Qualifier("securedDataGenerationService") final DataGenerationService dataGenerationService) {
    this.dataGenerationService = dataGenerationService;
  }

  @PostMapping
  public void generateRandomData(@PathVariable final String id) {
    dataGenerationService.generateRandomAnswers(id);
  }
}
