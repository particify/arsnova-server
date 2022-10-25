package net.particify.arsnova.core.service;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import net.particify.arsnova.core.model.Content;
import net.particify.arsnova.core.model.Room;

@Service
public class SecuredDuplicationService implements DuplicationService {
  private DuplicationService duplicationService;

  public SecuredDuplicationService(final DuplicationService duplicationService) {
    this.duplicationService = duplicationService;
  }

  @Override
  @PreAuthorize("hasPermission(#room, 'duplicate')")
  public Room duplicateRoomCascading(final Room room, final boolean temporary, final String newName) {
    return duplicationService.duplicateRoomCascading(room, temporary, newName);
  }

  @Override
  @PreAuthorize("hasPermission(#content, 'duplicate') and hasPermission(#contentGroupId, 'contentgroup', 'update')")
  public Content duplicateContent(
      final Content content,
      final String contentGroupId) {
    return duplicationService.duplicateContent(content, contentGroupId);
  }
}
