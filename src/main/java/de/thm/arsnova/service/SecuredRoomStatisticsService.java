package de.thm.arsnova.service;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import de.thm.arsnova.model.RoomStatistics;

@Service
public class SecuredRoomStatisticsService implements RoomStatisticsService {
	private RoomStatisticsService roomStatisticsService;

	public SecuredRoomStatisticsService(final RoomStatisticsService roomStatisticsService) {
		this.roomStatisticsService = roomStatisticsService;
	}

	@Override
	@PreAuthorize("hasPermission(#roomId, 'room', 'read')")
	public RoomStatistics getRoomStatistics(final String roomId) {
		return roomStatisticsService.getRoomStatistics(roomId);
	}
}
