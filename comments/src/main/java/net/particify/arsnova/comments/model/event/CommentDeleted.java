package net.particify.arsnova.comments.model.event;

public class CommentDeleted extends WebSocketEvent<CommentDeletedPayload> {
  public CommentDeleted() {
    super(CommentDeleted.class.getSimpleName());
  }

  public CommentDeleted(CommentDeletedPayload p, String roomId) {
    super(CommentDeleted.class.getSimpleName(), roomId);
    this.payload = p;
  }
}
