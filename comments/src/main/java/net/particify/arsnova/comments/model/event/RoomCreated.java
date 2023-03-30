package net.particify.arsnova.comments.model.event;

import java.util.UUID;

public class RoomCreated extends WebSocketEvent<RoomCreatedPayload> {
  public RoomCreated() {
    super(RoomCreated.class.getSimpleName());
  }

  public RoomCreated(RoomCreatedPayload p, UUID id) {
    super(RoomCreated.class.getSimpleName(), id);
    this.payload = p;
  }
}
