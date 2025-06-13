package net.particify.arsnova.core.controller;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import net.particify.arsnova.core.model.RoomSettings;
import net.particify.arsnova.core.service.RoomSettingsService;

@RestController
@EntityRequestMapping(RoomSettingsController.REQUEST_MAPPING)
public class RoomSettingsController extends AbstractEntityController<RoomSettings> {
  protected static final String REQUEST_MAPPING = "/room/{roomId}/roomsettings";
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
    return roomSettings != null ? roomSettings : roomSettingsService.create(new RoomSettings(roomId));
  }
}
