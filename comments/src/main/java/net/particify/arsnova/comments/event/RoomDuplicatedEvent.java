package net.particify.arsnova.comments.event;

import java.util.UUID;

class RoomDuplicatedEvent {
  private UUID originalRoomId;
  private UUID duplicatedRoomId;

  public UUID getOriginalRoomId() {
    return originalRoomId;
  }

  public void setOriginalRoomId(final UUID originalRoomId) {
    this.originalRoomId = originalRoomId;
  }

  public UUID getDuplicatedRoomId() {
    return duplicatedRoomId;
  }

  public void setDuplicatedRoomId(final UUID duplicatedRoomId) {
    this.duplicatedRoomId = duplicatedRoomId;
  }
}
