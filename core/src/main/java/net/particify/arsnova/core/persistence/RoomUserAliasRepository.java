package net.particify.arsnova.core.persistence;

import java.util.List;

import net.particify.arsnova.core.model.RoomUserAlias;

public interface RoomUserAliasRepository extends CrudRepository<RoomUserAlias, String> {
  List<RoomUserAlias> findByRoomId(String roomId);

  RoomUserAlias findByRoomIdAndUserId(String roomId, String userId);

  List<RoomUserAlias> findByUserId(String userId);
}
