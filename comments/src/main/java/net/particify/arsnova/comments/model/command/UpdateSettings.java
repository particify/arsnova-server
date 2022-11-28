package net.particify.arsnova.comments.model.command;

public class UpdateSettings extends WebSocketCommand<UpdateSettingsPayload> {
  public UpdateSettings() {
    super(UpdateSettings.class.getSimpleName());
  }
  public UpdateSettings(UpdateSettingsPayload p) {
    super(UpdateSettings.class.getSimpleName());
    this.payload = p;
  }
}