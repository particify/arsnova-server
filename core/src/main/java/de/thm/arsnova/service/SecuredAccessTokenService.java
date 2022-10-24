package de.thm.arsnova.service;

import java.util.Optional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import de.thm.arsnova.model.AccessToken;
import de.thm.arsnova.security.RoomRole;

@Service
public class SecuredAccessTokenService extends AbstractSecuredEntityServiceImpl<AccessToken>
    implements AccessTokenService, SecuredService {
  private final AccessTokenService accessTokenService;

  public SecuredAccessTokenService(final AccessTokenService accessTokenService) {
    super(AccessToken.class, accessTokenService);
    this.accessTokenService = accessTokenService;
  }

  @Override
  @PreAuthorize("hasPermission(#roomId, 'room', 'owner')")
  public AccessToken generate(
      final String roomId,
      final RoomRole roomRole) {
    return accessTokenService.generate(roomId, roomRole);
  }

  @Override
  @PreAuthorize("hasPermission(#roomId, 'room', 'owner')")
  public AccessToken generateAndSendInvite(
      final String roomId,
      final RoomRole roomRole,
      final String emailAddress) {
    return accessTokenService.generateAndSendInvite(roomId, roomRole, emailAddress);
  }

  @Override
  @PreAuthorize("isAuthenticated")
  public Optional<RoomRole> redeemToken(final String roomId, final String token) {
    return accessTokenService.redeemToken(roomId, token);
  }
}
