package net.particify.arsnova.comments.model.command;

public class CreateSettings extends WebSocketCommand<CreateSettingsPayload> {
  public CreateSettings() {
    super(CreateSettings.class.getSimpleName());
  }
  public CreateSettings(CreateSettingsPayload p) {
    super(UpdateComment.class.getSimpleName());
    this.payload = p;
  }
}