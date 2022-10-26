package de.thm.arsnova.service;

import de.thm.arsnova.model.Content;
import de.thm.arsnova.model.Room;

public interface DuplicationService {
	Room duplicateRoomCascading(Room room, boolean temporary);

	Content duplicateContent(Content content, String contentGroupId);
}
