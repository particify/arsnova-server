package de.thm.arsnova.service.comment.handler;

import de.thm.arsnova.service.comment.exception.ForbiddenException;
import de.thm.arsnova.service.comment.model.Settings;
import de.thm.arsnova.service.comment.model.command.CreateSettings;
import de.thm.arsnova.service.comment.model.command.CreateSettingsPayload;
import de.thm.arsnova.service.comment.model.command.UpdateSettings;
import de.thm.arsnova.service.comment.model.command.UpdateSettingsPayload;
import de.thm.arsnova.service.comment.security.PermissionEvaluator;
import de.thm.arsnova.service.comment.service.SettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SettingsCommandHandler {
    private static final Logger logger = LoggerFactory.getLogger(SettingsCommandHandler.class);

    private final SettingsService service;
    private final PermissionEvaluator permissionEvaluator;

    @Autowired
    public SettingsCommandHandler(
            SettingsService service,
            PermissionEvaluator permissionEvaluator
    ) {
        this.service = service;
        this.permissionEvaluator = permissionEvaluator;
    }

    public Settings handle(CreateSettings command) {
        logger.debug("Got new command: {}", command);

        CreateSettingsPayload payload = command.getPayload();

        if (!permissionEvaluator.isOwnerOrAnyTypeOfModeratorForRoom(payload.getRoomId())) {
            throw new ForbiddenException();
        }

        Settings newSettings = new Settings();
        newSettings.setRoomId(payload.getRoomId());
        newSettings.setDirectSend(payload.getDirectSend());

        Settings saved = service.create(newSettings);

        return saved;
    }

    public Settings handle(UpdateSettings command) {
        logger.debug("Got new command: {}", command);

        UpdateSettingsPayload payload = command.getPayload();

        if (!permissionEvaluator.isOwnerOrAnyTypeOfModeratorForRoom(payload.getRoomId())) {
            throw new ForbiddenException();
        }

        Settings settings = service.get(payload.getRoomId());
        settings.setRoomId(payload.getRoomId());
        settings.setDirectSend(payload.getDirectSend());

        Settings updated = service.update(settings);

        return updated;

    }
}
