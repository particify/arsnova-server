package de.thm.arsnova.service.comment.model;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.Objects;

@Entity
public class Settings {
    @Id
    private String roomId;
    private Boolean directSend;

    public Settings() {
    }

    public Boolean getDirectSend() {
        return directSend;
    }

    public void setDirectSend(Boolean directSend) {
        this.directSend = directSend;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    @Override
    public String toString() {
        return "Settings{" +
                "roomId='" + roomId + '\'' +
                ", directSend=" + directSend +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Settings settings = (Settings) o;
        return Objects.equals(directSend, settings.directSend);
    }

    @Override
    public int hashCode() {
        return Objects.hash(directSend);
    }
}
