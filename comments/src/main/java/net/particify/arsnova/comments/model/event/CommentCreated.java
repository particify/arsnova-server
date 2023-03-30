package net.particify.arsnova.comments.model.event;

import java.util.UUID;

public class CommentCreated extends WebSocketEvent<CommentCreatedPayload> {
  public CommentCreated() {
    super(CommentCreated.class.getSimpleName());
  }

  public CommentCreated(CommentCreatedPayload p, UUID id) {
    super(CommentCreated.class.getSimpleName(), id);
    this.payload = p;
  }
}
