package net.particify.arsnova.core.service;

import net.particify.arsnova.core.model.Content;
import net.particify.arsnova.core.model.Room;

public interface DuplicationService {
  Room duplicateRoomCascading(Room room, boolean temporary, String newName);

  Content duplicateContent(Content content, String contentGroupId);
}
