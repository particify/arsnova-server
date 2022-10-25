package net.particify.arsnova.comments.model;

import java.util.Objects;

public class Stats {
  private long commentCount;
  private long voteCount;

  public Stats() {
  }

  public Stats(final long commentCount, final long voteCount) {
    this.commentCount = commentCount;
    this.voteCount = voteCount;
  }

  public long getCommentCount() {
    return commentCount;
  }

  public void setCommentCount(final long commentCount) {
    this.commentCount = commentCount;
  }

  public long getVoteCount() {
    return voteCount;
  }

  public void setVoteCount(final long voteCount) {
    this.voteCount = voteCount;
  }

  @Override
  public String toString() {
    return "Stats{" +
        "commentCount=" + commentCount +
        ", voteCount=" + voteCount +
        '}';
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    final Stats stats = (Stats) o;
    return commentCount == stats.commentCount &&
        voteCount == stats.voteCount;
  }

  @Override
  public int hashCode() {
    return Objects.hash(commentCount, voteCount);
  }
}
