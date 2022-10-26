package de.thm.arsnova.service.comment.model.command;

public class UpdateSettings extends WebSocketCommand<UpdateSettingsPayload> {
    public UpdateSettings() {
        super(UpdateSettings.class.getSimpleName());
    }
    public UpdateSettings(UpdateSettingsPayload p) {
        super(UpdateSettings.class.getSimpleName());
        this.payload = p;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UpdateSettings that = (UpdateSettings) o;
        return this.getPayload().equals(that.getPayload());
    }
}