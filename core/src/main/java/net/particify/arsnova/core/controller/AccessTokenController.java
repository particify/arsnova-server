package net.particify.arsnova.core.controller;

import com.fasterxml.jackson.annotation.JsonView;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import net.particify.arsnova.core.model.AccessToken;
import net.particify.arsnova.core.model.Room;
import net.particify.arsnova.core.model.serialization.View;
import net.particify.arsnova.core.security.RoomRole;
import net.particify.arsnova.core.service.AccessTokenService;
import net.particify.arsnova.core.service.RoomService;

@RestController
@RequestMapping(AccessTokenController.REQUEST_MAPPING)
public class AccessTokenController extends AbstractEntityController<AccessToken> {
  public static final String REQUEST_MAPPING = "/room/{roomId}/access-token";
  public static final String GENERATE_MAPPING = "/generate";
  public static final String INVITE_MAPPING = "/invite";

  private AccessTokenService accessTokenService;
  private RoomService roomService;

  protected AccessTokenController(
      @Qualifier("securedAccessTokenService") final AccessTokenService accessTokenService,
      @Qualifier("securedRoomService") final RoomService roomService) {
    super(accessTokenService);
    this.accessTokenService = accessTokenService;
    this.roomService = roomService;
  }

  @Override
  protected String getMapping() {
    return REQUEST_MAPPING;
  }

  @PostMapping(GENERATE_MAPPING)
  public AccessToken generate(
      @PathVariable final String roomId,
      @RequestBody final AccessTokenRequestEntity accessTokenRequestEntity) {
    return accessTokenService.generate(
        roomId,
        accessTokenRequestEntity.role);
  }

  @PostMapping(INVITE_MAPPING)
  public void generateAndSendInvite(
      @PathVariable final String roomId,
      @RequestBody final AccessTokenRequestEntity accessTokenRequestEntity) {
    final Room room = roomService.get(roomId);
    accessTokenService.generateAndSendInvite(
        room,
        accessTokenRequestEntity.role,
        accessTokenRequestEntity.emailAddress);
  }

  @JsonView(View.Public.class)
  private static class AccessTokenRequestEntity {
    private RoomRole role;
    private String emailAddress;

    public void setRole(final RoomRole role) {
      this.role = role;
    }

    public void setEmailAddress(final String emailAddress) {
      this.emailAddress = emailAddress;
    }
  }
}
