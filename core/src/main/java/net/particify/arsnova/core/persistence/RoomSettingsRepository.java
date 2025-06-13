package net.particify.arsnova.core.persistence;

import net.particify.arsnova.core.model.RoomSettings;

public interface RoomSettingsRepository extends CrudRepository<RoomSettings, String> {
  RoomSettings findByRoomId(String roomId);
}
