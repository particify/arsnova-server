package de.thm.arsnova.service.comment.model;

import java.io.Serializable;
import java.util.Objects;

public class BonusTokenPK implements Serializable {
    protected String roomId;
    protected String commentId;
    protected String userId;

    public BonusTokenPK() {
    }

    public BonusTokenPK(String roomId, String commentId, String userId) {
        this.roomId = roomId;
        this.commentId = commentId;
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

    public String getCommentId() {
        return commentId;
    }

    public void setCommentId(String commentId) {
        this.commentId = commentId;
    }

    @Override
    public String toString() {
        return "BonusTokenPK{" +
                "roomId='" + roomId + '\'' +
                ", commentId='" + commentId + '\'' +
                ", userId='" + userId + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BonusTokenPK that = (BonusTokenPK) o;
        return Objects.equals(roomId, that.roomId) &&
                Objects.equals(commentId, that.commentId) &&
                Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(roomId, commentId, userId);
    }
}
