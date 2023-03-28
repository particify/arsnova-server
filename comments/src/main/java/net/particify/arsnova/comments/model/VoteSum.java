package net.particify.arsnova.comments.model;

import java.util.UUID;

public class VoteSum {
  private UUID commentId;
  private int sum;

  public VoteSum(final UUID commentId, final long sum) {
    this.commentId = commentId;
    this.sum = (int) sum;
  }

  public UUID getCommentId() {
    return commentId;
  }

  public int getSum() {
    return sum;
  }
}
