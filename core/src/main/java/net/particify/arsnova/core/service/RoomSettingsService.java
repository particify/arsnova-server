package net.particify.arsnova.core.service;

import net.particify.arsnova.core.model.RoomSettings;

public interface RoomSettingsService extends EntityService<RoomSettings> {
  RoomSettings getByRoomId(String roomId);
}
