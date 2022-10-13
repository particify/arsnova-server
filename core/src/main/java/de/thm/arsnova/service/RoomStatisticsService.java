package de.thm.arsnova.service;

import de.thm.arsnova.model.RoomStatistics;

public interface RoomStatisticsService {
  RoomStatistics getAllRoomStatistics(String roomId);

  RoomStatistics getPublicRoomStatistics(String roomId);
}
