package net.particify.arsnova.comments.model.event;

import net.particify.arsnova.comments.model.command.UpdateComment;

public class CommentUpdated extends WebSocketEvent<CommentUpdatedPayload> {
  public CommentUpdated() {
    super(UpdateComment.class.getSimpleName());
  }

  public CommentUpdated(CommentUpdatedPayload p, String id) {
    super(CommentUpdated.class.getSimpleName(), id);
    this.payload = p;
  }
}
