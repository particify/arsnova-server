package net.particify.arsnova.comments.model.event;

import java.util.UUID;

public class VoteCreated extends WebSocketEvent<VoteCreatedPayload> {
  public VoteCreated() {
    super(VoteCreated.class.getSimpleName());
  }

  public VoteCreated(VoteCreatedPayload p, UUID id) {
    super(VoteCreated.class.getSimpleName(), id);
    this.payload = p;
  }
}
