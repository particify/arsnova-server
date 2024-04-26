package net.particify.arsnova.core.service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import net.particify.arsnova.core.model.RoomUserAlias;

@Service
public class SecuredRoomUserAliasService
    extends AbstractSecuredEntityServiceImpl<RoomUserAlias>
    implements RoomUserAliasService, SecuredService {
  private RoomUserAliasService roomUserAliasService;

  public SecuredRoomUserAliasService(final RoomUserAliasService roomUserAliasService) {
    super(RoomUserAlias.class, roomUserAliasService);
    this.roomUserAliasService = roomUserAliasService;
  }

  @Override
  @PreAuthorize("hasPermission(#entity.userId, 'userprofile', 'owner') and "
      + "hasPermission(#entity.roomId, 'room', 'read')")
  public RoomUserAlias create(final RoomUserAlias entity) {
    return super.create(entity);
  }

  @Override
  @PreAuthorize("denyAll")
  public List<RoomUserAlias> getByRoomId(final String roomId) {
    return roomUserAliasService.getByRoomId(roomId);
  }

  @Override
  public RoomUserAlias getByRoomIdAndUserId(final String roomId, final String userId) {
    return roomUserAliasService.getByRoomIdAndUserId(roomId, userId);
  }

  @Override
  @PreAuthorize("denyAll")
  public List<RoomUserAlias> getByUserId(final String userId) {
    return roomUserAliasService.getByUserId(userId);
  }

  @Override
  @PreAuthorize("denyAll")
  public Map<String, RoomUserAlias> getUserAliasMappingsByRoomId(final String roomId, final Locale locale) {
    return roomUserAliasService.getUserAliasMappingsByRoomId(roomId, locale);
  }

  @Override
  @PreAuthorize("permitAll")
  public RoomUserAlias generateAlias(final Locale locale) {
    return roomUserAliasService.generateAlias(locale);
  }

  @Override
  @PreAuthorize("permitAll")
  public RoomUserAlias generateAlias(final int seed, final Locale locale) {
    return roomUserAliasService.generateAlias(seed, locale);
  }

  @Override
  public RoomUserAlias retrieveOrGenerateAlias(final String roomId, final String userId, final Locale locale) {
    return roomUserAliasService.retrieveOrGenerateAlias(roomId, userId, locale);
  }
}
