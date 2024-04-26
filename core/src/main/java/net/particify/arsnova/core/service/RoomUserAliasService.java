package net.particify.arsnova.core.service;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.particify.arsnova.core.model.RoomUserAlias;

public interface RoomUserAliasService extends EntityService<RoomUserAlias> {
  List<RoomUserAlias> getByRoomId(String roomId);

  RoomUserAlias getByRoomIdAndUserId(String roomId, String userId);

  List<RoomUserAlias> getByUserId(String userId);

  Map<String, RoomUserAlias> getUserAliasMappingsByRoomId(String roomId, Locale locale);

  RoomUserAlias generateAlias(Locale locale);

  RoomUserAlias generateAlias(int seed, Locale locale);

  RoomUserAlias retrieveOrGenerateAlias(String roomId, String userId, Locale locale);
}
