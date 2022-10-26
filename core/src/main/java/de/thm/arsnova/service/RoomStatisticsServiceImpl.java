package de.thm.arsnova.service;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import de.thm.arsnova.model.ContentGroup;
import de.thm.arsnova.model.RoomStatistics;

@Service
@Primary
public class RoomStatisticsServiceImpl implements RoomStatisticsService {
	final ContentGroupService contentGroupService;

	public RoomStatisticsServiceImpl(final ContentGroupService contentGroupService) {
		this.contentGroupService = contentGroupService;
	}

	@Override
	public RoomStatistics getAllRoomStatistics(final String roomId) {
		final RoomStatistics roomStatistics = new RoomStatistics();
		final List<ContentGroup> contentGroups = contentGroupService.getByRoomId(roomId);
		roomStatistics.updateFromContentGroups(contentGroups);
		return roomStatistics;
	}

	@Override
	public RoomStatistics getPublicRoomStatistics(final String roomId) {
		final RoomStatistics roomStatistics = new RoomStatistics();
		final List<ContentGroup> contentGroups = contentGroupService.getByRoomId(roomId).stream()
				.filter(cg -> cg.isPublished()).collect(Collectors.toList());;
		roomStatistics.updateFromContentGroups(contentGroups);
		return roomStatistics;
	}
}
