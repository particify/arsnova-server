package net.particify.arsnova.comments.model.event;

public class CommentPatched extends WebSocketEvent<CommentPatchedPayload> {
  public CommentPatched() {
    super(CommentPatched.class.getSimpleName());
  }

  public CommentPatched(CommentPatchedPayload p, String id) {
    super(CommentPatched.class.getSimpleName(), id);
    this.payload = p;
  }
}
