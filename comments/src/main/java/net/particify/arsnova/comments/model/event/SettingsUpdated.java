package net.particify.arsnova.comments.model.event;

import java.util.UUID;

import net.particify.arsnova.comments.model.command.UpdateSettings;
import net.particify.arsnova.comments.model.command.UpdateSettingsPayload;

public class SettingsUpdated extends WebSocketEvent<UpdateSettingsPayload> {

  public SettingsUpdated() {
    super(UpdateSettings.class.getSimpleName());
  }

  public SettingsUpdated(UpdateSettingsPayload p, UUID id) {
    super(UpdateSettings.class.getSimpleName(), id);
    this.payload = p;
  }
}
