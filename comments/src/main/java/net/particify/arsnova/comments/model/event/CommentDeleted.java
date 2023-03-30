package net.particify.arsnova.comments.model.event;

import java.util.UUID;

public class CommentDeleted extends WebSocketEvent<CommentDeletedPayload> {
  public CommentDeleted() {
    super(CommentDeleted.class.getSimpleName());
  }

  public CommentDeleted(CommentDeletedPayload p, UUID roomId) {
    super(CommentDeleted.class.getSimpleName(), roomId);
    this.payload = p;
  }
}
