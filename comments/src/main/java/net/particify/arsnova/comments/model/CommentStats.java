package net.particify.arsnova.comments.model;

import java.util.Objects;

public class CommentStats {
  private String roomId;
  private int ackCommentCount;
  private int unackCommentCount;

  public CommentStats() {
  }

  public CommentStats(final String roomId, final int ackCommentCount, final int unackCommentCount) {
    this.roomId = roomId;
    this.ackCommentCount = ackCommentCount;
    this.unackCommentCount = unackCommentCount;
  }

  public String getRoomId() {
    return roomId;
  }

  public void setRoomId(final String roomId) {
    this.roomId = roomId;
  }

  public int getAckCommentCount() {
    return ackCommentCount;
  }

  public void setAckCommentCount(final int ackCommentCount) {
    this.ackCommentCount = ackCommentCount;
  }

  public int getUnackCommentCount() {
    return unackCommentCount;
  }

  public void setUnackCommentCount(final int unackCommentCount) {
    this.unackCommentCount = unackCommentCount;
  }

  @Override
  public String toString() {
    return "CommentStats{" +
        "roomId='" + roomId + '\'' +
        ", ackCommentCount=" + ackCommentCount +
        ", unackCommentCount=" + unackCommentCount +
        '}';
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    final CommentStats that = (CommentStats) o;
    return ackCommentCount == that.ackCommentCount &&
        unackCommentCount == that.unackCommentCount &&
        Objects.equals(roomId, that.roomId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(roomId, ackCommentCount, unackCommentCount);
  }
}
