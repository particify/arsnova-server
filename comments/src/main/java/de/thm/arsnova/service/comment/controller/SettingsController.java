package de.thm.arsnova.service.comment.controller;

import de.thm.arsnova.service.comment.handler.SettingsCommandHandler;
import de.thm.arsnova.service.comment.model.Settings;
import de.thm.arsnova.service.comment.model.command.CreateSettings;
import de.thm.arsnova.service.comment.model.command.CreateSettingsPayload;
import de.thm.arsnova.service.comment.model.command.UpdateSettings;
import de.thm.arsnova.service.comment.model.command.UpdateSettingsPayload;
import de.thm.arsnova.service.comment.service.SettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletResponse;

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
