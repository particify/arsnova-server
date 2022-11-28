package net.particify.arsnova.comments.model.event;

public class CommentCreated extends WebSocketEvent<CommentCreatedPayload> {
  public CommentCreated() {
    super(CommentCreated.class.getSimpleName());
  }

  public CommentCreated(CommentCreatedPayload p, String id) {
    super(CommentCreated.class.getSimpleName(), id);
    this.payload = p;
  }
}
