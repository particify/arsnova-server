package net.particify.arsnova.core.service;

import java.util.Optional;

import net.particify.arsnova.core.model.AccessToken;
import net.particify.arsnova.core.model.Room;
import net.particify.arsnova.core.security.RoomRole;

public interface AccessTokenService extends EntityService<AccessToken> {
  AccessToken generate(String roomId, RoomRole roomRole);

  AccessToken generateAndSendInvite(Room room, RoomRole roomRole, String emailAddress);

  Optional<RoomRole> redeemToken(String roomId, String token);
}
