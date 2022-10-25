package net.particify.arsnova.comments.model.event;

public class CommentCreated extends WebSocketEvent<CommentCreatedPayload> {
  public CommentCreated() {
    super(CommentCreated.class.getSimpleName());
  }

  public CommentCreated(CommentCreatedPayload p, String id) {
    super(CommentCreated.class.getSimpleName(), id);
    this.payload = p;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    CommentCreated that = (CommentCreated) o;
    return this.getPayload().equals(that.getPayload());
  }
}
