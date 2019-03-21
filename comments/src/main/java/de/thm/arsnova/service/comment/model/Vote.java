package de.thm.arsnova.service.comment.model;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class Vote {
    @Id
    private String id;
    private String userId;
    private String commentId;
    private int vote;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public int getVote() {
        return vote;
    }

    public void setVote(int vote) {
        this.vote = vote;
    }

    @Override
    public String toString() {
        return "Vote{" +
                "id='" + id + '\'' +
                ", userId='" + userId + '\'' +
                ", commentId='" + commentId + '\'' +
                ", vote=" + vote +
                '}';
    }
}
