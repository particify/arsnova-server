package de.thm.arsnova.service;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import de.thm.arsnova.model.Content;
import de.thm.arsnova.model.Room;

@Service
public class SecuredDuplicationService implements DuplicationService {
	private DuplicationService duplicationService;

	public SecuredDuplicationService(final DuplicationService duplicationService) {
		this.duplicationService = duplicationService;
	}

	@Override
	@PreAuthorize("hasPermission(#room, 'duplicate')")
	public Room duplicateRoomCascading(final Room room) {
		return duplicationService.duplicateRoomCascading(room);
	}

	@Override
	@PreAuthorize("hasPermission(#content, 'duplicate') and hasPermission(#contentGroupId, 'contentgroup', 'update')")
	public Content duplicateContent(
			final Content content,
			final String contentGroupId) {
		return duplicationService.duplicateContent(content, contentGroupId);
	}
}
