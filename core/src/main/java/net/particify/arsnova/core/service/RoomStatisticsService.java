package net.particify.arsnova.core.service;

import net.particify.arsnova.core.model.RoomStatistics;

public interface RoomStatisticsService {
  RoomStatistics getAllRoomStatistics(String roomId);

  RoomStatistics getPublicRoomStatistics(String roomId);
}
