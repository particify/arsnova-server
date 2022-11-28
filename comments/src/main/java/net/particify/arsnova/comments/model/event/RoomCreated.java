package net.particify.arsnova.comments.model.event;

public class RoomCreated extends WebSocketEvent<RoomCreatedPayload> {
  public RoomCreated() {
    super(RoomCreated.class.getSimpleName());
  }

  public RoomCreated(RoomCreatedPayload p, String id) {
    super(RoomCreated.class.getSimpleName(), id);
    this.payload = p;
  }
}
