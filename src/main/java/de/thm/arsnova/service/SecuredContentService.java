package de.thm.arsnova.service;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import de.thm.arsnova.model.Content;

@Service
public class SecuredContentService extends AbstractSecuredEntityServiceImpl<Content>
		implements ContentService, SecuredService {
	private final ContentService contentService;

	public SecuredContentService(final ContentService contentService) {
		super(Content.class, contentService);
		this.contentService = contentService;
	}

	@Override
	@PreAuthorize("hasPermission(#roomId, 'room', 'read')")
	public List<Content> getByRoomId(final String roomId) {
		return contentService.getByRoomId(roomId);
	}

	@Override
	@PreAuthorize("hasPermission(#roomId, 'room', 'read')")
	public Iterable<Content> getByRoomIdAndGroup(final String roomId, final String group) {
		return contentService.getByRoomIdAndGroup(roomId, group);
	}

	@Override
	@PreAuthorize("hasPermission(#roomId, 'room', 'read')")
	public int countByRoomId(final String roomId) {
		return contentService.countByRoomId(roomId);
	}

	@Override
	@PreAuthorize("hasPermission(#roomId, 'room', 'read')")
	public int countByRoomIdAndGroup(final String roomId, final String group) {
		return contentService.countByRoomIdAndGroup(roomId, group);
	}
}
