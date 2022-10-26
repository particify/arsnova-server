package de.thm.arsnova.service;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.access.prepost.PreFilter;
import org.springframework.stereotype.Service;

import de.thm.arsnova.model.Announcement;

@Service
public class SecuredAnnouncementService extends AbstractSecuredEntityServiceImpl<Announcement>
		implements AnnouncementService, SecuredService {
	private AnnouncementService announcementService;

	public SecuredAnnouncementService(final AnnouncementService announcementService) {
		super(Announcement.class, announcementService);
		this.announcementService = announcementService;
	}

	@Override
	@PreAuthorize("hasPermission(#roomId, 'room', 'read')")
	public List<Announcement> getByRoomId(final String roomId) {
		return announcementService.getByRoomId(roomId);
	}

	@Override
	@PreFilter(value = "hasPermission(filterObject, 'room', 'read')", filterTarget = "roomIds")
	public List<Announcement> getByRoomIds(final List<String> roomIds) {
		return announcementService.getByRoomIds(roomIds);
	}
}
