package de.thm.arsnova.service.comment.model;

import java.io.Serializable;
import java.util.Objects;

public class BonusTokenPK implements Serializable {
    protected String roomId;
    protected String userId;

    public BonusTokenPK() {
    }

    public BonusTokenPK(String roomId, String userId) {
        this.roomId = roomId;
        this.userId = userId;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Override
    public String toString() {
        return "BonusTokenPK{" +
                "roomId='" + roomId + '\'' +
                ", userId='" + userId + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BonusTokenPK that = (BonusTokenPK) o;
        return Objects.equals(roomId, that.roomId) &&
                Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(roomId, userId);
    }
}
