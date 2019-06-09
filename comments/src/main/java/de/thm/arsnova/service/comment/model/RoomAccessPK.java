package de.thm.arsnova.service.comment.model;

import java.io.Serializable;
import java.util.Objects;

public class RoomAccessPK implements Serializable {
    protected String roomId;
    protected String userId;

    public RoomAccessPK() {}

    public RoomAccessPK(String roomId, String userId) {
        this.roomId = roomId;
        this.userId = userId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RoomAccessPK that = (RoomAccessPK) o;
        return Objects.equals(roomId, that.roomId) &&
                Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(roomId, userId);
    }
}
