package net.particify.arsnova.comments.model;

import java.util.Objects;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;

@Entity
@IdClass(VotePK.class)
public class Vote {
  @Id
  private String userId;
  @Id
  private String commentId;
  private int vote;

  public Vote() {
  }

  public Vote(final String userId, final String commentId, final int vote) {
    this.userId = userId;
    this.commentId = commentId;
    this.vote = vote;
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
        ", userId='" + userId + '\'' +
        ", commentId='" + commentId + '\'' +
        ", vote=" + vote +
        '}';
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    final Vote vote1 = (Vote) o;
    return vote == vote1.vote &&
        Objects.equals(userId, vote1.userId) &&
        Objects.equals(commentId, vote1.commentId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userId, commentId, vote);
  }
}
