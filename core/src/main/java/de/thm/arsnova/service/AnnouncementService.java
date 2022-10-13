package de.thm.arsnova.service;

import java.util.List;

import de.thm.arsnova.model.Announcement;

public interface AnnouncementService extends EntityService<Announcement> {
  List<Announcement> getByRoomId(String roomId);

  List<Announcement> getByRoomIds(List<String> roomIds);
}
