package net.particify.arsnova.comments.model.event;

import net.particify.arsnova.comments.model.command.UpdateSettings;
import net.particify.arsnova.comments.model.command.UpdateSettingsPayload;

public class SettingsUpdated extends WebSocketEvent<UpdateSettingsPayload> {

  public SettingsUpdated() {
    super(UpdateSettings.class.getSimpleName());
  }

  public SettingsUpdated(UpdateSettingsPayload p, String id) {
    super(UpdateSettings.class.getSimpleName(), id);
    this.payload = p;
  }
}
