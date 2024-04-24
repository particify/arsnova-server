package net.particify.arsnova.core.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import net.particify.arsnova.core.model.RoomUserAlias;
import net.particify.arsnova.core.security.User;
import net.particify.arsnova.core.service.RoomUserAliasService;

@RestController
@EntityRequestMapping(RoomUserAliasController.REQUEST_MAPPING)
public class RoomUserAliasController extends AbstractEntityController<RoomUserAlias> {
  protected static final String REQUEST_MAPPING = "/room/{roomId}/user-alias";
  private static final String GENERATE_MAPPING = "/-/generate";

  private RoomUserAliasService roomUserAliasService;

  public RoomUserAliasController(
      @Qualifier("securedRoomUserAliasService") final RoomUserAliasService roomUserAliasService) {
    super(roomUserAliasService);
    this.roomUserAliasService = roomUserAliasService;
  }

  @Override
  protected String getMapping() {
    return REQUEST_MAPPING;
  }

  @PostMapping(GENERATE_MAPPING)
  public RoomUserAlias generateRoomUserAlias(
      @PathVariable final String roomId,
      @AuthenticationPrincipal final UserDetails userDetails,
      final HttpServletRequest request) {
    final String acceptLanguage = request.getHeader("Accept-Language");
    final Locale requestLocale = acceptLanguage != null && acceptLanguage.length() >= 2
        ? Locale.of(acceptLanguage.substring(0, 2))
        : Locale.of("en");
    final User user = switch (userDetails) {
      case User u -> u;
      default -> throw new IllegalStateException("Unexpected type for UserDetails.");
    };
    return roomUserAliasService.retrieveOrGenerateAlias(roomId, user.getId(), requestLocale);
  }
}
