package net.particify.arsnova.comments.model;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class VotePK implements Serializable {
  protected UUID userId;
  protected UUID commentId;

  public VotePK() {
  }

  public VotePK(final UUID userId, final UUID commentId) {
    this.userId = userId;
    this.commentId = commentId;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    final VotePK votePK = (VotePK) o;
    return Objects.equals(userId, votePK.userId) &&
        Objects.equals(commentId, votePK.commentId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userId, commentId);
  }
}
