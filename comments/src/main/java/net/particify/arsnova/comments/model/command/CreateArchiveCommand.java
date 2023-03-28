package net.particify.arsnova.comments.model.command;

import java.util.Set;
import java.util.UUID;

public class CreateArchiveCommand {
  private String name;
  private UUID roomId;
  private Set<String> commentIds;

  public CreateArchiveCommand() {
  }

  public CreateArchiveCommand(final String name, final UUID roomId, final Set<String> commentIds) {
    this.name = name;
    this.roomId = roomId;
    this.commentIds = commentIds;
  }

  public String getName() {
    return name;
  }

  public void setName(final String name) {
    this.name = name;
  }

  public UUID getRoomId() {
    return roomId;
  }

  public void setRoomId(final UUID roomId) {
    this.roomId = roomId;
  }

  public Set<String> getCommentIds() {
    return commentIds;
  }

  public void setCommentIds(final Set<String> commentIds) {
    this.commentIds = commentIds;
  }

  @Override
  public String toString() {
    return "CreateArchiveCommand{" +
        "name='" + name + '\'' +
        ", roomId='" + roomId + '\'' +
        ", commentIds=" + commentIds +
        '}';
  }
}
