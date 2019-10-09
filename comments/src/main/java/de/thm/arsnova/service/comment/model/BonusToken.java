package de.thm.arsnova.service.comment.model;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import java.util.Objects;

@Entity
@IdClass(BonusTokenPK.class)
public class BonusToken {
    @Id
    private String roomId;
    @Id
    private String userId;
    private String token;

    public BonusToken() {
    }

    public BonusToken(String roomId, String userId, String token) {
        this.roomId = roomId;
        this.userId = userId;
        this.token = token;
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

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    @Override
    public String toString() {
        return "BonusToken{" +
                "roomId='" + roomId + '\'' +
                ", userId='" + userId + '\'' +
                ", token='" + token + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BonusToken that = (BonusToken) o;
        return Objects.equals(roomId, that.roomId) &&
                Objects.equals(userId, that.userId) &&
                Objects.equals(token, that.token);
    }

    @Override
    public int hashCode() {
        return Objects.hash(roomId, userId, token);
    }
}
