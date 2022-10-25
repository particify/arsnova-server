package net.particify.arsnova.core.persistence;

import java.util.List;

import net.particify.arsnova.core.model.Announcement;

public interface AnnouncementRepository extends CrudRepository<Announcement, String> {
  List<String> findIdsByRoomId(String roomId);

  List<String> findIdsByRoomIds(List<String> roomIds);
}
