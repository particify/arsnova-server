package de.thm.arsnova.service;

import java.util.Optional;

import de.thm.arsnova.model.AccessToken;
import de.thm.arsnova.security.RoomRole;

public interface AccessTokenService extends EntityService<AccessToken> {
  AccessToken generate(String roomId, RoomRole roomRole);

  AccessToken generateAndSendInvite(String roomId, RoomRole roomRole, String emailAddress);

  Optional<RoomRole> redeemToken(String roomId, String token);
}
