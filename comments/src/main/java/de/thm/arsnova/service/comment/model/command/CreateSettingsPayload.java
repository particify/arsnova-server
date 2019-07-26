package de.thm.arsnova.service.comment.model.command;

import de.thm.arsnova.service.comment.model.Settings;
import de.thm.arsnova.service.comment.model.WebSocketPayload;

import java.util.Objects;

public class CreateSettingsPayload implements WebSocketPayload {
    private String roomId;
    private Boolean directSend;

    public CreateSettingsPayload() {
    }

    public CreateSettingsPayload(Settings settings) {
        roomId = settings.getRoomId();
        directSend = settings.getDirectSend();
    }

    public CreateSettingsPayload(String roomId, Boolean directSend) {
        this.roomId = roomId;
        this.directSend = directSend;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public Boolean getDirectSend() {
        return directSend;
    }

    public void setDirectSend(Boolean directSend) {
        this.directSend = directSend;
    }

    @Override
    public String toString() {
        return "CreateSettingsPayload{" +
                "roomId='" + roomId + '\'' +
                ", directSend='" + directSend + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CreateSettingsPayload that = (CreateSettingsPayload) o;
        return Objects.equals(roomId, that.roomId) &&
                Objects.equals(directSend, that.directSend);
    }

    @Override
    public int hashCode() {
        return Objects.hash(roomId, directSend);
    }
}
