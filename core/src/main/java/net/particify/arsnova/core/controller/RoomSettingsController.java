package net.particify.arsnova.core.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import net.particify.arsnova.core.model.RoomSettings;
import net.particify.arsnova.core.service.RoomSettingsService;

@RestController
@EntityRequestMapping(RoomSettingsController.REQUEST_MAPPING)
public class RoomSettingsController extends AbstractEntityController<RoomSettings> {
  protected static final String REQUEST_MAPPING = "/room/{roomId}/roomsettings";
  private static final Logger logger = LoggerFactory.getLogger(RoomSettingsController.class);
  private RoomSettingsService roomSettingsService;

  protected RoomSettingsController(
      @Qualifier("securedRoomSettingsService") final RoomSettingsService roomSettingsService) {
    super(roomSettingsService);
    this.roomSettingsService = roomSettingsService;
  }

  @Override
  protected String getMapping() {
    return REQUEST_MAPPING;
  }

  @GetMapping("/")
  public RoomSettings getByRoomId(@PathVariable final String roomId) {
    final RoomSettings roomSettings = roomSettingsService.getByRoomId(roomId);
    if (roomSettings != null) {
      return roomSettings;
    }
    RoomSettings newRoomSettings = new RoomSettings(roomId);
    try {
      newRoomSettings = roomSettingsService.create(newRoomSettings);
    } catch (final AccessDeniedException e) {
      logger.debug("Cannot create room settings.", e);
    }
    return newRoomSettings;
  }
}
