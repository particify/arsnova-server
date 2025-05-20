package net.particify.arsnova.core.service;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import net.particify.arsnova.core.model.RoomSettings;

@Service
public class SecuredRoomSettingsService
    extends AbstractSecuredEntityServiceImpl<RoomSettings>
    implements RoomSettingsService, SecuredService {
  private RoomSettingsService roomSettingsService;

  public SecuredRoomSettingsService(
      final RoomSettingsServiceImpl roomSettingsService) {
    super(RoomSettings.class, roomSettingsService);
    this.roomSettingsService = roomSettingsService;
  }

  @Override
  @PreAuthorize("hasPermission(#entity.roomId, 'room', 'owner')")
  public RoomSettings create(final RoomSettings entity) {
    return roomSettingsService.create(entity);
  }

  @Override
  @PreAuthorize("hasPermission(#entity.roomId, 'room', 'owner')")
  public RoomSettings update(final RoomSettings entity) {
    return roomSettingsService.update(entity);
  }

  @Override
  @PreAuthorize("hasPermission(#roomId, 'room', 'read')")
  public RoomSettings getByRoomId(final String roomId) {
    return roomSettingsService.getByRoomId(roomId);
  }
}
