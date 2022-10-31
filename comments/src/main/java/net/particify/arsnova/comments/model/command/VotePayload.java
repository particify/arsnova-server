package net.particify.arsnova.comments.model.command;

import java.util.Objects;

import net.particify.arsnova.comments.model.WebSocketPayload;

public class VotePayload implements WebSocketPayload {
  private String userId;
  private String commentId;

  public VotePayload() {
  }

  public VotePayload(String userId, String commentId) {
    this.userId = userId;
    this.commentId = commentId;
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
    return "VotePayload{" +
        "userId='" + userId + '\'' +
        ", commentId='" + commentId + '\'' +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    VotePayload that = (VotePayload) o;
    return Objects.equals(userId, that.userId) &&
        Objects.equals(commentId, that.commentId);
  }

  @Override
  public int hashCode() {

    return Objects.hash(userId, commentId);
  }
}
