package net.particify.arsnova.comments.model.event;

public class VoteCreated extends WebSocketEvent<VoteCreatedPayload> {
  public VoteCreated() {
    super(VoteCreated.class.getSimpleName());
  }

  public VoteCreated(VoteCreatedPayload p, String id) {
    super(VoteCreated.class.getSimpleName(), id);
    this.payload = p;
  }
}
