package net.particify.arsnova.comments.model.event;

import java.util.UUID;

public class CommentPatched extends WebSocketEvent<CommentPatchedPayload> {
  public CommentPatched() {
    super(CommentPatched.class.getSimpleName());
  }

  public CommentPatched(CommentPatchedPayload p, UUID id) {
    super(CommentPatched.class.getSimpleName(), id);
    this.payload = p;
  }
}
