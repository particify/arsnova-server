package de.thm.arsnova.service.comment.model.command;

public class CreateSettings extends WebSocketCommand<CreateSettingsPayload> {
    public CreateSettings() {
        super(CreateSettings.class.getSimpleName());
    }
    public CreateSettings(CreateSettingsPayload p) {
        super(UpdateComment.class.getSimpleName());
        this.payload = p;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CreateSettings that = (CreateSettings) o;
        return this.getPayload().equals(that.getPayload());
    }
}