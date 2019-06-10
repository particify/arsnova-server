package de.thm.arsnova.service.comment.model;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import java.util.Objects;

@Entity
@IdClass(RoomAccessPK.class)
public class RoomAccess {

    public enum Role {
        OWNER,
        EDITING_MODERATOR,
        EXECUTIVE_MODERATOR
    }

    @Id
    private String roomId;
    @Id
    private String userId;
    private Role role;

    public RoomAccess() {
    }

    public RoomAccess(String roomId, String userId, Role role) {
        this.roomId = roomId;
        this.userId = userId;
        this.role = role;
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

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    @Override
    public String toString() {
        return "RoomAccess{" +
                ", roomId='" + roomId + '\'' +
                ", userId='" + userId + '\'' +
                ", role=" + role +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RoomAccess that = (RoomAccess) o;
        return Objects.equals(roomId, that.roomId) &&
               Objects.equals(userId, that.userId) &&
               role == that.role;
    }

    @Override
    public int hashCode() {
        return Objects.hash(roomId, userId, role);
    }
}
