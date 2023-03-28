package net.particify.arsnova.comments.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import java.util.Objects;
import java.util.UUID;

@Entity
@IdClass(VotePK.class)
public class Vote {
  @Id
  private UUID userId;
  @Id
  private UUID commentId;
  private int vote;

  public Vote() {
  }

  public Vote(final UUID userId, final UUID commentId, final int vote) {
    this.userId = userId;
    this.commentId = commentId;
    this.vote = vote;
  }

  public UUID getUserId() {
    return userId;
  }

  public void setUserId(UUID userId) {
    this.userId = userId;
  }

  public UUID getCommentId() {
    return commentId;
  }

  public void setCommentId(UUID commentId) {
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
