package net.particify.arsnova.comments.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import net.particify.arsnova.comments.handler.SettingsCommandHandler;
import net.particify.arsnova.comments.model.Settings;
import net.particify.arsnova.comments.model.command.CreateSettings;
import net.particify.arsnova.comments.model.command.CreateSettingsPayload;
import net.particify.arsnova.comments.model.command.UpdateSettings;
import net.particify.arsnova.comments.model.command.UpdateSettingsPayload;
import net.particify.arsnova.comments.service.SettingsService;

@RestController("SettingsController")
@RequestMapping(SettingsController.REQUEST_MAPPING)
public class SettingsController extends AbstractEntityController {
  private static final Logger logger = LoggerFactory.getLogger(SettingsController.class);

  protected static final String REQUEST_MAPPING = "/room/{roomId}/settings";

  private final SettingsService service;
  private final SettingsCommandHandler commandHandler;

  @Autowired
  public SettingsController(
      SettingsService service,
      SettingsCommandHandler commandHandler
  ) {
    this.service = service;
    this.commandHandler = commandHandler;
  }

  @GetMapping(GET_MAPPING)
  public Settings get(@PathVariable String id) {
    return service.get(id);
  }

  @PostMapping(POST_MAPPING)
  @ResponseStatus(HttpStatus.CREATED)
  public Settings post(
      @PathVariable final String roomId,
      @RequestBody final Settings settings,
      final HttpServletResponse httpServletResponse
  ) {
    final CreateSettingsPayload payload  = new CreateSettingsPayload(settings);
    final CreateSettings command = new CreateSettings(payload);

    Settings s = commandHandler.handle(roomId, command);

    final String uri = UriComponentsBuilder.fromPath(REQUEST_MAPPING).path(GET_MAPPING)
        .buildAndExpand(s.getRoomId()).toUriString();
    httpServletResponse.setHeader(HttpHeaders.LOCATION, uri);
    httpServletResponse.setHeader(ENTITY_ID_HEADER, s.getRoomId());

    return s;
  }

  @PutMapping(PUT_MAPPING)
  public Settings put(
      @PathVariable final String roomId,
      @RequestBody final Settings entity,
      final HttpServletResponse httpServletResponse
  ) {
    UpdateSettingsPayload p = new UpdateSettingsPayload(entity);
    UpdateSettings command = new UpdateSettings(p);

    Settings s = this.commandHandler.handle(roomId, command);

    httpServletResponse.setHeader(ENTITY_ID_HEADER, s.getRoomId());

    return s;
  }

}
