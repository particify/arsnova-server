package net.particify.arsnova.comments.model.event;

public class CommentPatched extends WebSocketEvent<CommentPatchedPayload> {
  public CommentPatched() {
    super(CommentPatched.class.getSimpleName());
  }

  public CommentPatched(CommentPatchedPayload p, String id) {
    super(CommentPatched.class.getSimpleName(), id);
    this.payload = p;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    CommentPatched that = (CommentPatched) o;
    return this.getPayload().equals(that.getPayload());
  }
}
