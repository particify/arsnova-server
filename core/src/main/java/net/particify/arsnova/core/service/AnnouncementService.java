package net.particify.arsnova.core.service;

import java.util.List;

import net.particify.arsnova.core.model.Announcement;

public interface AnnouncementService extends EntityService<Announcement> {
  List<Announcement> getByRoomId(String roomId);

  List<Announcement> getByRoomIds(List<String> roomIds);
}
