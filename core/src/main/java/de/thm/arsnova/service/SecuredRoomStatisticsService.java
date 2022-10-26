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
	@PreAuthorize("hasPermission(#roomId, 'room', 'read-extended')")
	public RoomStatistics getAllRoomStatistics(final String roomId) {
		return roomStatisticsService.getAllRoomStatistics(roomId);
	}

	@Override
	@PreAuthorize("hasPermission(#roomId, 'room', 'read')")
	public RoomStatistics getPublicRoomStatistics(final String roomId) {
		return roomStatisticsService.getPublicRoomStatistics(roomId);
	}
}
