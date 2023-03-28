package net.particify.arsnova.comments.event;

import java.util.Date;
import java.util.UUID;

public class ImportEvent {
  private UUID roomId;
  private UUID creatorId;
  private String body;
  private Date timestamp;
  private boolean read;

  public UUID getRoomId() {
    return roomId;
  }

  public void setRoomId(UUID roomId) {
    this.roomId = roomId;
  }

  public UUID getCreatorId() {
    return creatorId;
  }

  public void setCreatorId(UUID creatorId) {
    this.creatorId = creatorId;
  }

  public String getBody() {
    return body;
  }

  public void setBody(String body) {
    this.body = body;
  }

  public Date getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(Date timestamp) {
    this.timestamp = timestamp;
  }

  public boolean isRead() {
    return read;
  }

  public void setRead(boolean read) {
    this.read = read;
  }
}
