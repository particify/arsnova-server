package de.thm.arsnova.service;

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
	public Room duplicateRoomCascading(final Room room) {
		return duplicationService.duplicateRoomCascading(room);
	}

	@Override
	public Content duplicateContent(
			final Content content,
			final String contentGroupId) {
		return duplicationService.duplicateContent(content, contentGroupId);
	}
}
